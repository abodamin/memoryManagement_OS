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
	private static LinkedList<Process> processQueue = new LinkedList<Process>();
	private static int task;
	private static int memorySize;

	public static void main(String[] args) {
        File file = new File(args[0]);

        try {
	        Scanner sc;
	        sc = new Scanner(file);
        	
	        if (sc.hasNext()){

	        	String firstLine = sc.nextLine();
	        	String[] memorySize_task = firstLine.split(" ");

	        	memorySize = Integer.parseInt(memorySize_task[0]);  // Get the total memory size
	        	task = Integer.parseInt(memorySize_task[2]); 		// 0 = segmentation, 1 = paging 

		        while (sc.hasNext()){
	        		String nextJob = sc.nextLine();
	        		
					// this list is : [A, size, pid, text, data, heap]
	        		String[] newProcess_List = nextJob.split(" ");

		        	// Actions: A = add / D = delete / P = print
		        	if (newProcess_List.length == 1) {
		        		// For Print: [P]
		        		String action = newProcess_List[0];

		        		Process newProcess = new Process(action);
						processQueue.add(newProcess);

		        	} else if (newProcess_List.length == 2){
						// For Delete: [D, 1]
		        		String action = newProcess_List[0];
		        		int pid = Integer.parseInt(newProcess_List[2]);
		        		
		        		// Make your new 'process' object & add to queue
						Process newProcess = new Process(action, pid);
						processQueue.add(newProcess);

		        	} else {
        				// For Add: [A, size, pid, text, data, heap]
        				String action = newProcess_List[0];
		        		int pid = Integer.parseInt(newProcess_List[2]);
						int textSegment = Integer.parseInt(newProcess_List[4]); // Creating the data segment
						int dataSegment = Integer.parseInt(newProcess_List[5]); // Creating the data segment
						int heapSegment = Integer.parseInt(newProcess_List[6]); // Creating the heap segment

						// Make your new 'process' object & add to queue
						Process newProcess = new Process(action, pid, textSegment, dataSegment, heapSegment);
						processQueue.add(newProcess);
		        	} // if, elif,else
		        	
	        	} // EOWhile
	        } // EOIf

	        // close your scanner
	        sc.close();

	        // Send all of the information to the MemoryManagement Class
	        MemoryManagement newManage = new MemoryManagement(memorySize, task, processQueue);

        } catch (FileNotFoundException e) {
        	e.printStackTrace();
        }

    } //EOmain

} // EOF