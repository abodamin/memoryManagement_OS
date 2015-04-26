Programming Assignment 3
========================
Group Member(s): Alice Yang, Giovanna Diaz

INCLUDED IN THIS .ZIP:
----------------------

Main Files:
- - - - - -
	>> MemoryManagement.java - The file that does the grunt work of the CPU
		> Hole / Action / Page / Segment: Not separate files, but the objects for their respective
					   types. These can be found at the bottom of
					   MemoryManagement.java
	>> TestMemoryManagement.java - main file that reads through supplied text file and passes off 
	information to MemoryManagement.java

	>> Process.java - Class that creates Process Objects to be used in TestMemoryManagement.java 
	and MemoryManagement.java

Testing Files:
- - - - - - - -
	>> sample.txt - supplied test file
	>> sample2.txt - simple test file for SEGMENTATION
	>> sample3.txt - simple test file for PAGING
	>> internalFrag_segmentation.txt - test file with minimal internal fragmentation within segmentation
	>> internalFrag_paging.txt - test file with minimal internal fragmentation within segmentation

Other:
- - - -
	>> README.txt - this text file


COMPILING INSTRUCTIONS:
-----------------------
First enter the folder via terminal. Then compile all of the java files:
$ javac *.java

>> In order to begin the simulation, supply the TestMemoryManagement.java 
>>    with at .txt file of processes.
$ java TestMemoryManagement sample.txt


SUMMARY OF DESIGN CHOICES:
--------------------------
You can read above which files are included in this .zip. The “TestMemoryManagemet.java” Class reads in
the supplied sample.txt file (as well as our other written tests) and sends an ArrayList<Process> to our 
MemoryManagement.java file. We decided to separate the reading and calculations between the two files to 
keep the implementation cleaner.

Once in MemoryManagement.java, we implement the different commands based on the given policy. We begin 
creating segment/page objects and allocating them accordingly. We are using switches since it seemed to be 
the most elegant option if we are to keep everything in one file, despite having to break at the end of 
each case. You will find that we have created objects for Actions, Pages, Segments, and Holes. This seemed 
to be the simplest way to contain all of the information needed for each. (As well as account for the mix 
of Types (int, string, etc.) that was in the information. We felt as though an an array of arrays added a 
level of abstraction that was not necessary and a bit confusing.

We wanted to implement the best-fit policy. We have one ArrayList of holes that we iterate through in order
to find the best hole for our segment. The pages just are just assigned to the first free available.
You will see that there are two HashMaps - one for segments, and one for pages. These hashmaps are used
to keep track of the mappings inbetween the processes and their respctive segments/pages. These are particularly
useful when in our printMemoryState() method we want to print the segments/pages in order of their processes.

Overall, our MemoryManagement.java class follows the given sudocode. We did give a lot of attention to how 
we were going to structure the different classes, and all of the information that each component needed. 
We also tried to keep efficiency in mind, but had to make some sacrifices.


DOCUMENT THE PROBLEMS:
----------------------
/* update this later when the problems arise and we are close to the deadline */


ANSWERS TO PART 4:
------------------
4.1) In your implementation of segmentation which of the best fit, first fit, or worst fit memory allocation policy 
do you use to find a free memory region for each segment? Why did you pick this policy? (5)
> We implemented the best fit policy to find the empty segments. We chose this policy because it uses the 
RAM more efficiently and it doesn’t waste as much space as the other policies that we could have used. We
are have O(n) efficiency beacuse of our loops.
 

- - - - - - - - - - - - - - - -
4.2) What data structures and search algorithm do you use for searching through the list of holes (in 
segmentation)? You will get 5 points for implementing a brute force linear search. If you implement 
anything more efficient than this, you will get full 10 points. (10)
> We are using an ArrayList to keep track of the Hole objects that we have made. We are using the brute 
force linear search in order to find the best fit hole for the segment. 


- - - - - - - - - - - - - - - -
4.3) For segmentation, what data structure do you use to track the start location of each segment? (2)
> For segmentation, we have a “Segment” object... 
	Segment(int pid, String type, int segmentSize, int base, int limit).
  For more detail of each input... 
  	> pid = Process ID, used for finding all the segments from the same process
	> type = text / heap / data, this is used when printing the RAM information
	> segmentSize = the size of the segment
	> base & limit = base and limit registers of the segment
  We use these objects to track all the information related to the segment, and this includes the start 
  location. We are using a HashMap in order to keep track of the mappings of the segments created
  from each process.


- - - - - - - - - - - - - - - -
4.4) For paging, explain your choice of algorithm and data structure for tracking free pages. (5)
> For our paging, we have a “Page” object…
	Page(int pid, int nVirtual, int pageSize)
  For more detail about each input…
	> pid = process ID, used for finding all the pages from the same process
	> nVirtual = the virtual address of the page
	> pageSize = the size of the page - while we allocate each page to 32 bytes (the standard size for 
		     each page), we also want to keep track of the internal fragmentation. This keeps 
		     track since: (32 - pageSize) = internalFragmentation for the page
  All of the pages are stored in our Page Array - we chose to use an array since there will only be a set 
  number of pages as determined by the RAM and the fixed page size.


- - - - - - - - - - - - - - - -
4.5) In paging, what data structure do you use for tracking what physical page is mapped to each virtual 
page? (2)
> As you can see in the previous answer to (4.4), we have a Page object which contains the virtual page reference. 
The index in to the page array is the physical page number.


- - - - - - - - - - - - - - - -
4.6) How do the levels of internal and external fragmentation compare when you run the sample input in sample.txt with each of your allocators? Why is this the case? (5 + 5)
> When we run the sample.txt with SEGMENTATION we find that…

> When we run the sample.txt with PAGING we find that…



- - - - - - - - - - - - - - - -
4.7) Write an input test case where the Segmentation allocator has little internal fragmentation. Explain 
why this test case produces the result you see. (3)
> From the instructions, “When a segment is allocated within a hole, if the remaining space is less than 
16 bytes then the segment should be allocated the full hole.”

You can find our test file: “internalFrag_segmentation.txt”. The file starts out by completely filling the 
ram so that there is NO internal fragmentation at all. One of the processes is deleted, and then another 
allocated such that there is minimal internal fragmentation.


- - - - - - - - - - - - - - - -
4.8) Write another test case where the Paging allocator sees lot of internal fragmentation. Explain why 
this test case produces the result you see. (3)
> You can find our test file: “internalFrag_Paging.txt”. This file adds processes that contain a lot of 
internal fragmentation - where the standard page size is 32 bytes, just as with sample.txt.






// EOF