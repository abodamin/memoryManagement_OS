/*
 * Giovanna Diaz, Alice Yang
 * Programming Assignment #3
 * Operating Systems - Spring 2015
 *
 * Input(s): .txt file of jobs
 * Return(s): 
 */
import java.util.*;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.*;

class MemoryManagement {
    
    int bytes;
    int policy;
    LinkedList<Process> processQueue;
    int failedAllocations_noMemory = 0;
    int failedAllocations_externalFragmentation = 0;

    /* HoleLists for Segmentation */
	private ArrayList<Hole> holeList_byOrder = new ArrayList<Hole>();
	private ArrayList<Hole> holeList_bySize = new ArrayList<Hole>();

	/* HoleList for Paging */
	private ArrayList<Hole> holeList = new ArrayList<Hole>();

	/* ArrayLists for Segments and Pages */
	private ArrayList<Segment> segmentList = new ArrayList<Segment>();
	private Page[] pageList;

	public MemoryManagement(int bytes, int policy, LinkedList<Process> processQueue) { 
		// intialize memory - base 0, limit GivenBytes
		holeList.add(new Hole(0, bytes-1));
		this.bytes = bytes;
		this.policy = policy;
		this.processQueue = processQueue;

		// intialize memory with these many bytes.
		holeList_byOrder.add(new Hole(0, bytes-1));
		holeList_bySize.add(new Hole(0, bytes-1));

		run();
	}
	
	public void run() {
		// Use segmentation if policy==0, paging if policy==1
		for (Process process: processQueue) {
			// process info: A, size, pid, text, data, heap
			switch (process.getAction()) {
				case "A": // add process
					switch (policy) {
						case 0:	// segmentation
							int[] segmentList = process.getSegments();
							int pid = process.getPid();
							boolean segmentInserted;
							
							// try to insert every segment
							for (int segment: segmentList) {
								// segment = size of the segment from the int[] that is returned


// CHANGE THE WAY THE SEGMENT IS PASSED IN -> NEED TO INCLUDE TYPE OF SEGMENT

								segmentInserted = allocate(pid, segment);

								// deallocate process if segment doesn't fit, you're out of RAM
								if (segmentInserted == false) {
									deallocate(pid);
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
									break;
								} //EOif
							}//EOif

							
							// Get rest of pages
							for (int i = 0; i < pages; i++ ){
								pageInserted = allocate(pid, i, pageSize);

								// IF the page doesn't fit, then you're out of RAM
								if (pageInserted == false){
									deallocate(pid);
									break;
								} //EOif
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
		} // EOFor
	} // EORun


	/**
	*	Inserts a page into a page list if there is space left.
	*	
	*	@param	 page	
	*	@return  whether or not there was space for the page 
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
	*	Inserts a segment into a hole.
	* 	If segment doesn't take up all the space,
	* 	the leftover space becomes another hole.
	*	
	*	@param	 segment 	
	*	@param	 hole  
	*/
	public void insertSegmentInHole(Segment segment, Hole hole) {
		// assign base and limit registers
		segment.setBase(hole.getBase());
		segment.setLimit(hole.getBase() + segment.getSize() - 1);

		// add segment to list of segments
		addSegmentToSortedList(segment);
		
		int leftoverSpace = hole.getSize() - segment.getSize();
		if (leftoverSpace > 0) {
			// use leftover space to create new hole
			int newHoleBase = hole.getBase() + segment.getSize();
			int newHoleLimit = hole.getLimit();
			Hole newHole = new Hole(newHoleBase, newHoleLimit);
			// add hole
			addHole(newHole);
		}
	}

	/*
	*
	*
	*/
	public void addHoleToSortedSizeList(Hole hole, int size) {
		if (holeList_bySize.isEmpty()) {
			holeList_bySize.add(hole);
		} else {
			for (int i = 0; i < holeList_bySize.size(); i++) {
				if (size < holeList_bySize.get(i).getSize()) {
					holeList_bySize.add(i, hole);
					break;
				}
			}
		}
	}

	public void addSegmentToSortedList(Segment segment) {
		int base = segment.getBase();
		// if segment list is empty or segment belongs to end of list
		if (segmentList.isEmpty()) {
			segmentList.add(segment);
		} else if (base > segmentList.get(segmentList.size()-1).getBase()) {
			// segment belongs to end of list
			segmentList.add(segment);
		} else {
			Segment curSegment;
			int curBase;
			// add segment using insert sort
			for (int i = 0; i < segmentList.size(); i++) {
				curBase = segmentList.get(i).getBase();
				if (base < curBase) {
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

		for (int i = 0; i < holeList_byOrder.size(); i++) {
			Hole checkHole = holeList_byOrder.get(i);
			int checkHoleBase = checkHole.getBase();
			int checkHoleLimit = checkHole.getLimit();
			
			// check if hole needs to combine with hole before
			if (holeBase == checkHoleLimit + 1) {
				// merge hole to end of first hole
				iHoleBefore = i;
				hole.setBase(checkHoleBase);

				if ((i+1)<holeList_byOrder.size()) {
					// next hole exists
					checkHoleBase = holeList_byOrder.get(i+1).getBase();
					
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
		} //EoFor
	
		// remove holes to merge and add hole
		int iHoleInsert; // where the new hole should be added in list
		if (iHoleAfter != -1 && iHoleBefore != -1) {

			if (iHoleAfter != -1) {	// merge with hole after
				iHoleInsert = iHoleAfter;
				Hole holeAfter = holeList_byOrder.remove(iHoleAfter);
				holeList_bySize.remove(iHoleAfter);
			}
			
			if (iHoleBefore != -1) { // merge with hole before
				iHoleInsert = iHoleBefore;
				Hole holeBefore = holeList_byOrder.remove(iHoleBefore);
				holeList_bySize.remove(iHoleBefore);
			} 

			if (iHoleInsert == holeList_byOrder.size()-1) {
				// hole goes to end of list
				holeList_byOrder.add(hole);
			} else {
			// add hole to end of list of ordered holes
			holeList_byOrder.add(iHoleInsert, hole);
			} 
		} else {
			holeList_byOrder.add(hole);
			holeList_byOrder.remove(iHoleBefore);
		} 

		if (iHoleInsert == holeList_byOrder.size()-1) {
			holeList_byOrder.add(iHoleInsert, hole);
		} else { // add hole to end of list of ordered holes
			holeList_byOrder.add(hole); 
		}

		// add hole to end of list of size ordered holes
		addHoleToSortedSizeList(hole, hole.getSize());
	
	} // EOAddHole

	/**
	*	Tries to insert a segment into RAM.
	*	
	*	@param	 size of segment or page in bytes	
	*	@Return  whether or not segment could be inserted
	*/
	public boolean allocate(int pid, String type, int bytes) { 
		// Allocate the segment
		// Find the right Hole
		// Get the base and limit of that hole
		
		boolean allocated = false;
		// Segment(int pid, String type, int segmentSize)
		Segment newSegment = new Segment(pid, type, bytes);
		for (Hole hole: holeList_bySize) {
			if(bytes < hole.getSize()) {
				insertSegmentInHole(newSegment, hole);
				allocated = true;
				break;
			}
		}
		return allocated;
				
		// RE-SORT THE LIST!


		//If using the paging allocator, simply ignore the segment size variables 
		//Return 1 if successful 
		//Return -1 if unsuccessful 
		//Print an error indicating whether there wasn't sufficient memory or whether you 	//..ran into external fragmentation
		
		// try to find a hole bigger than block
		// if none, return false
		// if found, call insertSegmentInHole

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
		boolean found;
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
					int pagePid = pageList[i].getPid();
					if (pagePid == deallocatePid){
						// remove page
						pageList[i] = null;
						found = true;
					} //EOif
				} //EOif
				break;
		}
		// return 1 if successful, -1 otherwise with an error message
		// ^ returning a boolean would make more sense than int
		return found;
	}

	public void printMemoryState() { 
		// print out current state of memory
		// the output will depend on the memory allocator being used.

		int allocatedSpace = 0;
		int freeSpace = 0;
		switch (policy) {
			case 0:	// segmentation
				// Memory size = 1024 bytes, allocated bytes = 179, free = 845
				// There are currently 10 holes and 3 active process
				//
				// Hole list:
				// 	hole 1: start location = 0, size = 202
				// ...
				//
				// Process list:
				// process id=34, size & allocation=95
				// 	text start=202, size=25
				// 	data start=356, size=16
				// 	heap start=587, size=54
				//
				// process id=39, size=55 allocation=65
				// ...
				//
				// Total Internal Fragmentation = 10 bytes
				// Failed allocations (No memory) = 2
				// Failed allocations (External Fragmentation) = 7 
				for (Segment segment: segmentList) {
					allocatedSpace += segment.getSize();
				}

				for(Hole hole: holeList){
					freeSpace += hole.getSize();
				}

				System.out.println("Memory size = "+bytes+", allocated bytes = "+allocatedSpace+", free = "+freeSpace);
				System.out.println("There are currently "+holeList.size()+" holes and "+segmentList.size()+" active processes.");
				System.out.println("Hole List:");
				for(int i = 0; i < holeList.size(); i++){
					Hole hole = holeList[i];
					System.out.println("Hole "+i+": start location = "+hole.getBase()+", size = "+hole.getSize());
				}

				System.out.println("Process List:");
				for(int i = 0; i< segmentList.size(); i++){
					Segment segment = segmentList.get(i);
					// pid 3, text, start: 234, size: 2305
					System.out.println("Process ID "+segment.getPid()+", "+segment.getType()+" start = "+segment.getBase()+", size = "+segment.getSize());
				}
				
				System.out.println("Total Internal Fragmentation = ");
				System.out.println("Failed allocations (No memory) = ");
				System.out.println("Failed allocations (External Fragmentation) = ");
				
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
						freeSpace += 32;
					} else { // this slot has a page
						page = pageList[i];
						allocatedPages += 1;
						allocatedSpace += 32;
						// use a hash to store information on processes?
						// { pid -> page }

						internalFragmentation += (32 - page.getPageSize());
					}
				}

				System.out.println("Memory size = "+bytes+", total pages = "+(bytes/32));
				
				break;
		} //EOSwitch

	}

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
		public int getSize() { return limit - base; }
	} //EOHole

	/*
	/** Page Class 
	  * Creates a page object / keeps track of wasted space (by the size)
	  * inputs: pageSize (how much of the page is actually being used), pid (the process ID)
	  *         takesFulLSpace (boolean if it takes up all of 32 or not)
	  **/
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

	/* Segment Class
	 * creates a new segment object
	 * input: segmentSize, pid (The Process ID)
	 */
	public class Segment{
		private int pid;		
		private int segmentSize;
		private int base;
		private int limit;
		private String type;
		
		public Segment( int pid, String type, int segmentSize) { 
			this.pid = pid;
			this.type = type;
			this.segmentSize = segmentSize;
		} 

		public Segment( int pid, String type, int segmentSize, int base, int limit) { 
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
		public void setBase(int base){
			this.base = base;
		}
		public void setLimit(int limit){
			this.limit = limit;
		}
	} // EOSegment

	/* Action Class
	 * creates a new action object
	 * @param: Action (A/P/D), pid (The Process ID)
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
