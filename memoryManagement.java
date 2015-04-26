/*
 * Giovanna Diaz, Alice Yang
 * Programming Assignment #3
 * Operating Systems - Spring 2015
 * 
 *  
 */
import java.util.*;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.*;

class MemoryManagement {
    /* Global Variables */
    int bytes;
    int policy;
    int failedAllocations_noMemory = 0;
    int failedAllocations_externalFragmentation = 0;

    /* Queue for Processes - comes from TestMemoryManagement.java */
    LinkedList<Process> processQueue;

	/* HoleList for Paging */
	private ArrayList<Hole> holeList = new ArrayList<Hole>();

	/* ArrayLists for Segments and Pages */
	private ArrayList<Segment> segmentList = new ArrayList<Segment>();
	private Page[] pageList;


	/**
	*	MemoryManagement
	*   Constructor for MemoryManagement.java
	* 	Initializes and calls run() that begins to allocate / deallocate depending on the policy
	*		
	*	@param	 bytes, policy, processQueue  
	*/
	public MemoryManagement(int bytes, int policy, LinkedList<Process> processQueue) { 
		// intialize memory - base 0, limit GivenBytes
		this.bytes = bytes;
		this.policy = policy;
		this.processQueue = processQueue;

		// intialize memory with these many bytes.
		holeList.add(new Hole(0, bytes-1));

		run();
	}

	/**
	*   run()
	*   Begins allocating / deallocated 
	*/
	public void run() {
		if (policy == 1) { // paging
			// set up page list
			int nPages = this.bytes / 32;
			pageList = new Page[nPages];
		}

		// Use segmentation if policy==0, paging if policy==1
		for (Process process: processQueue) {
			if (!hasEnoughMemory(process)) {
				failedAllocations_noMemory++;
			} else { // not enough memory/space to add process

				// process info: A, size, pid, text, data, heap
				switch (process.getAction()) {
					case "A": // add process
						switch (policy) {
							case 0:	// segmentation


								int[] segmentSizeList = process.getSegments();
								String[] segmentTypeList = process.getSegmentTypes();
								int pid = process.getPid();
								boolean segmentInserted;
								
								for (int i = 0; i < segmentSizeList.length; i++) {
									segmentInserted = allocate(pid, segmentTypeList[i], segmentSizeList[i]);

									// deallocate process if segment doesn't fit, you're out of RAM
									if (segmentInserted == false) {
										deallocate(pid);
										failedAllocations_externalFragmentation++;
										break;
									}
								}
								break;
							case 1:	// paging
								int pageSize = 32;
								int totalSize = process.getSize();
								pid = process.getPid();
								boolean pageInserted;
								
								int remainder = (totalSize%3);
								int pages = (totalSize - remainder)/pageSize;

								// Get Remainder
								if (remainder > 0){
									int nVirtual = pages + 1;
									pageInserted = allocate(pid, nVirtual,remainder);
									
									// IF the page doesn't fit, then you're out of RAM
									if (pageInserted == false){
										deallocate(pid);
										failedAllocations_externalFragmentation++;
										break;
									}
								}//EOif

								
								// Get rest of pages
								for (int i = 0; i < pages; i++ ){
									pageInserted = allocate(pid, i, pageSize);

									// IF the page doesn't fit, then you're out of RAM
									if (pageInserted == false){
										deallocate(pid);
										failedAllocations_externalFragmentation++;
										break;
									}
								} //EOfor
								break;
							}//EOSwitch
							break;
					
					case "D": // delete process
							deallocate(process.getPid());
							break;
					case "P": // print 
							printMemoryState();
							break;
					default:
							break;
				} // EOswitch
				
			}
		} // EOFor
	} // EORun

	/**
	*
	*
	*
	*/
	public boolean hasEnoughMemory(Process process) {
		int processSize = process.getSize();
		if (processSize <= this.bytes) {
			return true;
		}
		return false;
	}

	/**
	*	Inserts a page into a page list if there is space left.
	*	
	*	@param	 page	
	*	@return  Whether or not there was space for the page 
	*/
	public boolean insertPage(Page page) {
		// add page of pages to list of pages
		for (int i = 0; i < pageList.length; i++) {

			// check if slot is empty
			if(pageList[i]==null) {

				// insert page in slot
				pageList[i] = page;
				return true;
			}
		}
		return false;
	}


	/**
	*   insertSegmentInHole()
	*	Inserts a segment into a hole.
	* 	If segment doesn't take up all the space,
	* 	the leftover space becomes another hole.
	*	
	*	@param	 Segment 	
	*	@param	 Hole  
	*/
	public void insertSegmentInHole(Segment segment, Hole hole) {
		// remove hole
		holeList.remove(hole);

		// check if any space leftover for new hole
		int leftoverSpace = hole.getSize() - segment.getSize();
		if (leftoverSpace > 16) { // there is more than 16 bytes leftover

			// assign segment base and limit registers
			segment.setBase(hole.getBase());
			segment.setLimit(hole.getBase() + segment.getSize() - 1);
		
			// use leftover space to create new hole
			int newHoleBase = hole.getBase() + segment.getSize();
			int newHoleLimit = hole.getLimit();
			Hole newHole = new Hole(newHoleBase, newHoleLimit);
			
			// add new hole
			addHole(newHole);
		
		} else { // segment takes up full hole

			// update segment base and limit registers
			segment.setBase(hole.getBase());
			segment.setLimit(hole.getLimit());

			// set internal fragmentation of segment
			segment.setInternalFrag(leftoverSpace);
		}
		
		// add segment to list of segments
		addSegmentToSortedList(segment);
	}

	/**
	* addSegmentToSortedList()
	* Adds a segment to the sorted list of segments 
	*
	* @param segment
	*/
	public void addSegmentToSortedList(Segment segment) {
		int base = segment.getBase();
		// if segment list is empty or segment belongs to end of list
		if (segmentList.isEmpty()) {
			segmentList.add(segment);
		} else if (base >= segmentList.get(segmentList.size()-1).getBase()) {
			// segment belongs to end of list
			segmentList.add(segment);
		} else {
			Segment curSegment;
			int curBase;
			// add segment using insert sort
			for (int i = 0; i < segmentList.size(); i++) {
				curBase = segmentList.get(i).getBase();
				if (base <= curBase) {
					segmentList.add(i, segment);
					break;
				}
			}
		}
	}

	/**
	*	Inserts hole into both list of holes
	* 	lists: sorted by hole size, sorted by hole order
	*		
	*	@param	 hole  
	*/
	public void addHole(Hole hole) {
		// check if hole needs to be combined with other hole
		int holeBase = hole.getBase();
		int holeLimit = hole.getLimit();


		// place holder for merged hole info
		int iHoleBefore = -1;
		int iHoleAfter = -1;

		// where the new hole should be added in list
		int iHoleInsert = -1;	

		for (int i = 0; i < holeList.size(); i++) {
			Hole checkHole = holeList.get(i);
			int checkHoleBase = checkHole.getBase();
			int checkHoleLimit = checkHole.getLimit();
			
			// check if hole needs to combine with hole before
			if (holeBase == checkHoleLimit + 1) {
				// merge hole to end of first hole
				iHoleBefore = i;
				hole.setBase(checkHoleBase);

				if ((i+1)<holeList.size()) {
					// next hole exists
					checkHoleBase = holeList.get(i+1).getBase();
					
					// check if hole needs to combine with hole after
					if (holeLimit == checkHoleBase - 1) {
						// merge with hole after
						iHoleAfter = i;
						hole.setLimit(checkHole.getLimit());
					}
				}
				break;

			// check if hole needs to combine with hole after
			} else if (holeLimit == checkHoleBase - 1) {
				// merge with hole after
				iHoleAfter = i;
				hole.setLimit(checkHoleLimit);
				break;
			}

			// check if hole comes before current hole
			if (holeLimit < checkHoleLimit) {
				iHoleInsert = i;
				break;
			}
		} //EoFor
	
		// remove holes to merge and add hole
		if (iHoleAfter != -1 && iHoleBefore != -1) {
			iHoleInsert = -1; 

			if (iHoleAfter != -1) {	// merge with hole after
				iHoleInsert = iHoleAfter;
				holeList.remove(iHoleAfter);
			}
			
			if (iHoleBefore != -1) { // merge with hole before
				iHoleInsert = iHoleBefore;
				holeList.remove(iHoleBefore);
			} 

			if (iHoleInsert == holeList.size()-1) {
				// hole goes to end of list
				holeList.add(hole);
			} else {
			// add hole to end of list of ordered holes
			holeList.add(iHoleInsert, hole);
			} 
		
		} else if (iHoleInsert != -1) {
			// hole comes before another hole
			holeList.add(iHoleInsert, hole);
		
		} else {
			// hole comes after all other holes
			holeList.add(hole);
		}
	} // EOAddHole

	/**
	*	Tries to insert a segment into RAM.
	*	
	*	@param	 size of segment or page in bytes	
	*	@Return  whether or not segment could be inserted
	*/
	public boolean allocate(int pid, String type, int bytes) { 		
		// Segment(int pid, String type, int segmentSize)
		Segment newSegment = new Segment(pid, type, bytes);
		int leftoverSpace = Integer.MAX_VALUE;
		int difference;
		Hole holeToInsert = null;
		for (Hole hole: holeList) {
			difference = hole.getSize() - bytes;
			// if segment fits in hole and is smaller than previously chosen hole
			if (difference >= 0 && difference < leftoverSpace) {
				holeToInsert = hole;
				leftoverSpace = difference;
			}

		}
		if (holeToInsert != null) {
			insertSegmentInHole(newSegment, holeToInsert);
			return true;
		} 
		return false;
	}

	/**
	*	Creates and tries to insert a page into RAM.
	*	
	*	@param	 size of page in bytes	
	*	@Return  whether or not PAGE could be inserted
	*/
	public boolean allocate(int pid, int nVirtual, int bytes) {
		Page page = new Page(pid, nVirtual, bytes);
		return insertPage(page);
	}

	public boolean deallocate(int deallocatePid) { 
		boolean found = false;
		switch (policy) {
			case 0:	// segmentation
				Segment segment;
				for (int i = segmentList.size()-1; i >= 0; i--){
					segment = segmentList.get(i);
					int segmentPid = segment.getPid(); 	    // Get the segment pid for each segment in the list
					if (segmentPid == deallocatePid){  									 // check to deallocate
						found = true;
						addHole(new Hole(segment.getBase(), segment.getLimit()));	   	 // create a hole
						segmentList.remove(segment);   									 // remove the segment
					} //EOif
				}//EOif

				break;
			case 1:	// paging
				for (int i = 0; i < pageList.length; i++) {
					if (pageList[i] != null) {	// page in slot
						int pagePid = pageList[i].getPid();
						
						if (pagePid == deallocatePid){	// check page ID
						// remove page
						pageList[i] = null;
						found = true;
						} //EOif
					}
					
				} //EOif
				break;
		}
		return found;
	}

	/**
	* printMemoryState()
	* When there is a "P" in the action list, it prints out the current
	* state of the memory
	*
	*/
	public void printMemoryState() { 
		// print out current state of memory
		// the output will depend on the memory allocator being used.

		int allocatedSpace = 0;
		int freeSpace = 0;
		switch (policy) {
			case 0:	// segmentation
				for (Segment segment: segmentList) {
					allocatedSpace += segment.getSize();
				}

				for(Hole hole: holeList){
					freeSpace += hole.getSize();
				}

				System.out.println("Memory size = "+bytes+", allocated bytes = "+allocatedSpace+", free = "+freeSpace);
				System.out.println("There are currently "+holeList.size()+" holes and "+segmentList.size()+" active processes.");
				System.out.println("Hole List:");

				// Printing out the Holes
				for(int i = 0; i < holeList.size(); i++){
					Hole hole = holeList.get(i);
					System.out.println("Hole "+i+": start location = "+hole.getBase()+", size = "+hole.getSize());
				}

				// Printing out the segments for each process
				System.out.println("Process List:");
				for(int i = 0; i< segmentList.size(); i++){
					Segment segment = segmentList.get(i);
					// pid 3, text, start: 234, size: 2305
					System.out.println("Process ID "+segment.getPid()+", "+segment.getType()+" start = "+segment.getBase()+", size = "+segment.getSize());
				}
				
				// Printing out the internal fragmentation
				int internalSegmentFrag = 0;
				for (Segment segment: segmentList){
					internalSegmentFrag += segment.getInternalFrag();
				}
				System.out.println("Total Internal Fragmentation = "+internalSegmentFrag);
				
				// Printing the failed allocations due to the 2 reasons
				System.out.println("Failed allocations (No memory) = "+failedAllocations_noMemory);
				System.out.println("Failed allocations (External Fragmentation) = "+failedAllocations_externalFragmentation);
				
				break;
			case 1:	// paging
				// Memory size = 1024 bytes, total pages = 32
				// allocated pages = 6, free pages = 26
				// There are currently 3 active process
				// Free Page list:
				// 	2,6,7,8,9,10,11,12...
				//
				// Process list:
				// Process id=34, size=95 bytes, number of pages=3
				// 	Virt Page 0 -> Phys Page 0 used: 32 bytes
				// 	Virt Page 1 -> Phys Page 3 used: 32 bytes
				// 	Virt Page 2 -> Phys Page 4 used: 31 bytes
				//
				// Process id=39, size=55 bytes, number of pages=2
				// 	Virt Page 0 -> Phys Page 1 used: 32 bytes
				// 	Virt Page 1 -> Phys Page 13 used: 23 bytes
				//
				// Process id=46, size=29 bytes, number of pages=1
				// 	Virt Page 0 -> Phys Page 5 used: 29 bytes 
				//
				// Total Internal Fragmentation = 13 bytes
				// Failed allocations (No memory) = 2
				// Failed allocations (External Fragmentation) = 0 
				
				/*
				allocatedSpace = 0;
				for(Page page: pageList){
					allocatedSpace += page.getSize();
				}

				freeSpace = 0;
				for(Hole hole: holeList){
					freeSpace += hole.getSize();
				}

				System.out.println("Memory size = "+bytes+", total pages = "+(bytes/32));
				int freePages = (bytes/30) - pageList.size();
				System.out.println("Allocated pages = "+pageList.size()+", free pages = "+freePages);

				*/

				int allocatedPages = 0;
				int internalFragmentation = 0;
				LinkedList<Integer> freePageList = new LinkedList<Integer>();
				Page page;
				for (int i = 0; i < pageList.length; i++) {
					if (pageList[i] == null) { // this slot is free
						System.out.println("free");
						freeSpace += 32;
					} else { // this slot has a page
						page = pageList[i];
						System.out.println("Phys Page"+i+" pid = "+page.getPid());
						allocatedPages += 1;
						allocatedSpace += 32;
						// use a hash to store information on processes?
						// { pid -> page }

						internalFragmentation += (32 - page.getPageSize());
					}
				}

				System.out.println("Memory size = "+bytes+", total possible pages = "+(bytes/32));
				
				break;
		} //EOSwitch

	}

	/**
	* Hole()
	* Creates a new Hole object
	* 
	* @param base, limit
	*/
	public class Hole {
		private int base;
		private int limit;

		/* Hole constructor */
		public Hole (int base, int limit) {
			this.base = base;
			this.limit = limit;
		}

		public int getBase() { return base; }
		public int getLimit() { return limit; }
		public void setBase(int base) { this.base = base; }
		public void setLimit(int limit) { this.base = base; }	
		public int getSize() { return limit - base + 1; }
	} //EOHole


	/**
	* Page()
	* Creates a new Page Object
	* @param pid, nVirtual, pageSize
	*/
	public class Page{
		private int pageSize;
		private int pid;
		private int nVirtual;

		// takes SIZE to potentially keep track of wasted space
		public Page(int pid, int nVirtual, int pageSize) { 
			this.pageSize = pageSize;
			this.nVirtual = nVirtual;
			this.pid = pid;
		} 

		public int getPid() { return pid; }
		public int getnVirtual() { return nVirtual; }
		public int getPageSize() { return pageSize; }
		
	} //EOPage


	/**
	* Segment()
	* Creates a new Segment Object
	*
	* @param pid, type, segmentSize, (base), (limit)
	*/
	public class Segment{
		private int pid;		
		private int segmentSize;
		private int base;
		private int limit;
		private String type;
		private int internalFragmentation;

		public Segment( int pid, String type, int segmentSize) { 
			this.pid = pid;
			this.type = type;
			this.segmentSize = segmentSize;
		} 

		public Segment( int pid, String type, int segmentSize, int base, int limit ) { 
			this.type = type;
			this.base = base;
			this.limit = limit;
			this.segmentSize = segmentSize;
			this.pid = pid;
		} 

		public String getType(){ return type; }
		public int getSize(){ return segmentSize; }
		public int getPid(){ return pid; }
		public int getBase(){ return base; }
		public int getLimit(){ return limit; }
		public int getInternalFrag() {return internalFragmentation; }
		
		public void setBase(int base){
			this.base = base;
		}

		public void setLimit(int limit){
			this.limit = limit;
		}

		public void setInternalFrag(int newInternalFrag){
			this.internalFragmentation = newInternalFrag;
		}
	} // EOSegment

	/**
	* Segment()
	* Creates a new Action Object - applies pid for A and D
	* otherwise just allows a P to also be an action
	*
	* @param pid, action
	*/
	public class Action{
		private String action;
		private int pid;

		public Action(int pid, String action) { 
			this.action = action;
			this.pid = pid;
		}

		public Action(String action){
			this.action = action;
		}

		public String getAction() { return action; }
		public int getPid() { return pid; }
	} // EOAction

} // EOF
