Q) What kinds of file lead to lots of compressions?
The huffman algorithm works by creating a tree where the higher frequency 
characters are closer to the root and assigned shorter bit codes. The total 
size of a compressed file is based on the total sum of each characters 
frequency times its assigned bit size. Therefore files with repeated 
characters can be compressed much more as the most common character will 
be represented by the shortest bits.

Q) What kind of files had little or no compression?
Files with low or no compression will generally have many distinct characters 
rather than having many common characters. This is because the huffman tree 
will become balanced so more characters will be assigned bits close to the 
original bit word size.

Q) What happens when you try and compress a huffman code file?
There is very little compression as the redundancy in the file has been 
already removed. Additionally, an extra header will be inserted which 
increases file size negligibly. Another compression would result in the 
huffman algorithm trying to create a new huffman distribution; however, 
this was already achieved in the inital compression.

Waterloo Directory (images)
File                 Original Bits    Compressed Bits   Reduction (approx)
clegg.tif            17,192,768       16,276,760        5.3%
frymire.tif          29,650,448       17,508,744        40.9%
lena.tif             6,292,544        6,129,168         2.6%
monarch.tif          9,438,272        8,879,784         5.9%
peppers.tif          6,292,544        6,055,744         3.8%
sail.tif             9,438,272        8,684,008         8.0%
serrano.tif          11,987,312       9,021,160         24.7%
tulips.tif           9,438,272        9,086,888         3.7%

Observation: 
Most TIFF images showed only modest compression (2-8%); however, frymire.tif and
serrano.tif had significant compression. This seems typical for 
images which is either redundant or already efficiently compressed

BooksAndHTML Directory (.txt vs .html)
File                          Original Bits    Compressed Bits   Reduction (approx)
A7_Recursion.html             329,304          209,512           36.4%
CiaFactBook2000.txt           27,978,952       18,085,312        35.4%
jnglb10.txt                   2,336,472        1,348,944         42.3%
kjv10.txt                     34,760,160       19,918,144        42.7%
melville.txt                  657,120          378,912           42.3%
quotes.htm                    492,504          307,384           37.6%
rawMovieGross.txt             938,176          430,664           54.1%
revDictionary.txt             9,044,184        4,892,944         45.9%
syllabus.htm                  266,184          170,736           35.9%
ThroughTheLookingGlass.txt    1,505,592        882,344           41.4%

Observation: 
Text and HTML files consistently showed good compression rates (35-55%), as 
they contain many repeated characters and patterns that Huffman coding can 
compress efficently.

Recompression Analysis .hf to .hf.hf
When trying to compress already compressed files, the size often increased 
or decreased negligibly.

A7_Recursion.html.hf went from 209,512 bit to A7_Recursion.html.hf.hf with 210,720 bits which increased
CiaFactBook2000.txt.hf went from 18,085,312 bits to CiaFactBook2000.txt.hf.hf with 17,920,064 bits which slightly decreased

Observation: 
Huffman compression relies on the large distributions of frequency of characters, so when a 
file is compressed the resulting data has a much more uniform distribution, 
leaving little redundancy for a second compression to effectively work.
Additionaly overhead of the header often outweighs any tiny gain, which may lead to a larger file size.

