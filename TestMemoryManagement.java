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

class TestMemoryManagement {

	/* Pre-processed Queue for Segments */
	private static LinkedList<Action> actionQueue = new LinkedList<Action>(); 

	/* Pre-processed Queue for pages */
	private static LinkedList<Page> pageQueue = new LinkedList<Page>();

	/* Pre-processed Queue for Actions */
	private static LinkedList<Segment> segmentQueue = new LinkedList<Segment>(); // Queue for Storing Info


	public static void main(String[] args) {
        File file = new File(args[0]);

        try {
	        Scanner sc;
	        sc = new Scanner(file);
        	
	        if (sc.hasNext()){

	        	String firstLine = sc.nextLine();
	        	String[] memorySize_task = firstLine.split(" ");

	        	int memorySize = Integer.parseInt(memorySize_task[0]);  // Get the total memory size
	        	int task = Integer.parseInt(memorySize_task[2]); 		// 1 = segmentation, 2 = paging 

	        	if (task == 0) { // If SEGMENTATION pre-preprocess the list
		        		while (sc.hasNext()){
		        		String nextJob = sc.nextLine();
		        		
						// this list is : [A, size, pid, text, data, heap]
		        		String[] newProcess_List = nextJob.split(" ");
			        	
			        	// Actions: A = add / D = delete / P = print
			        	String action = newProcess_List[0];
			        	
			        	if (action.equals("A")){ // IF you are adding something
			        		int pid = Integer.parseInt(newProcess_List[2]);   // get the PID and create the "add" action
							Action actionObject = new Action(action, pid);
							actionQueue.add(actionObject);
						
							Segment dataSegment = new Segment(Integer.parseInt(newProcess_List[5]), pid); // Creating the data segment
							segmentQueue.add(dataSegment);
							
							Segment textSegment = new Segment(Integer.parseInt(newProcess_List[4]), pid); // Creating the data segment
							segmentQueue.add(dataSegment);

							Segment heapSegment = new Segment(Integer.parseInt(newProcess_List[6]), pid); // Creating the heap segment
							segmentQueue.add(dataSegment);

			        	} else if (action.equals("D")) { // Deleting... ex. "D 5"
			        		Action actionObject = new Action(action, Integer.parseInt(newProcess_List[1])); // create delete action w/ pid
			        		actionQueue.add(actionObject);

			        	} else { // You must be printing
			        		Action actionObject = new Action(action, 100);
			        	}
	        		} // EOWhile
	        	} else if (task == 1){ // If PAGING pre-preprocess the list, pages = 32 bytes
	        		while (sc.hasNext()){
		        		String nextJob = sc.nextLine();
		        		
						// this list is : [A, size, pid, text, data, heap]
		        		String[] newProcess_List = nextJob.split(" ");
			        	
			        	// Actions: A = add / D = delete / P = print
			        	String action = newProcess_List[0];
			        	
			        	if (action.equals("A")){ // IF you are adding something
			        		int pid = Integer.parseInt(newProcess_List[2]);   // get the PID and create the "add" action
							Action actionObject = new Action(action, pid);
							actionQueue.add(actionObject);
						
							int processSize = Integer.parseInt(newProcess_List[1]);

							boolean makingPages = true;
							while(makingPages == true){  // Creating new pages
								int newPage = processSize - 32;

								if( newPage > 0){
									Page pageObject = new Page(32, pid);
									pageQueue.add(pageObject);
								} else {
									Page pageObject = new Page(processSize%32, pid); 
									pageQueue.add(pageObject);
									makingPages = false;
								}//eoIf/else
							}//EOwhile
			        	} else if (action.equals("D")) { // Deleting... ex. "D 5"
			        		Action actionObject = new Action(action, Integer.parseInt(newProcess_List[1])); // create delete action w/ pid
			        		actionQueue.add(actionObject);
			        	} else { // You must be printing
			        		Action actionObject = new Action(action, 100);
			        	}
	        		} // EOWhile
	        	} else {
	        		System.out.println("You have not provided the a valid task");
	        	} // EO if/elif/else
	        } // EOIf
	        sc.close(); // close your scanner


        } catch (FileNotFoundException e) {
        	e.printStackTrace();
        }

    } //EOmain


	/** Page Class 
	  * Creates a page object / keeps track of wasted space (by the size)
	  *
	  * inputs: pageSize (how much of the page is actually being used), pid (the process ID)
	  *         takesFulLSpace (boolean if it takes up all of 32 or not)
	  **/
	public static class Page{
		private int pageSize;
		private int pid;

		// takes SIZE to potentially keep track of wasted space
		public Page(int pageSize, int pid) { 
			this.pageSize = pageSize;
			this.pid = pid;
		} 

		public int getPageSize() { return pageSize; }
		public int getPid() { return pid; }
	} //EOPage

	/* Segment Class
	 * creates a new segment object
	 *
	 * input: segmentSize, pid (The Process ID)
	 */
	public static class Segment{
		private int segmentSize;
		private int pid;
		
		/* Page Constructor */
		public Segment(int segmentSize, int pid) { 
			this.segmentSize = segmentSize;
			this.pid = pid;
		} 

		public int getSize(){ return segmentSize; }
		public int getPid(){ return pid; }
	} // EOSegment

	/* Action Class
	 * creates a new action object
	 *
	 * input: Action (A/P/D), pid (The Process ID)
	 */
	public static class Action{
		private String action;
		private int pid;

		/* Action Constructor */
		public Action(String action, int pid) { 
			this.action = action;
			this.pid = pid;
		}

		public String getAction() { return action; }
		public int getPid() { return pid; }

	} // EOAction

} // EOF