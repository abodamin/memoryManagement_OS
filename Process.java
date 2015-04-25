/* Process Class
 * Giovanna Diaz, Alice Yang
 * 
 * Creates the Process object that is sent from TestMemoryManagement.java to memoryManagement.java
 *
 */
class Process {
	private String action;
	private int pid;
	private int textSegment;
	private int dataSegment;
	private int heapSegment;

	/* Process Constructors:
	 *    1) For when you are printing
	 *    1) For when you are deleting something
	 *    3) For when you are adding something
	 */
	public Process(String action) { 
		this.action = action;
	}

	public Process(String action, int pid) { 
		this.action = action;
		this.pid = pid;
	}

	public Process(String action, int pid, int textSegment, int dataSegment, int heapSegment) { 
		this.action = action;
		this.pid = pid;
		this.textSegment = textSegment;
		this.dataSegment = dataSegment;
		this.heapSegment = heapSegment;
	}

	// Getters for all of the variables
	public String getAction() { return action; }
	public int getPid() { return pid; }
	public int getText() { return textSegment; }
	public int getData() { return dataSegment; }
	public int getHeap() { return heapSegment; }
	

	// Returns a segment with all the appropriate segment information
	public int[] getSegments() { 
		int segmentArray[] = {textSegment, dataSegment, heapSegment};
		return segmentArray; 
	}

	public String[] getSegmentTypes() {
		String segmentTypeArray[] = {"text", "data", "heap"};
		return segmentTypeArray;
	}

	// Returns the TOTAL size of all the segments
	public int getSize() { return textSegment+dataSegment+heapSegment; }

} // EOAction