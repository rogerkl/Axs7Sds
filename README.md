# Akai X7000/S700 sample dump
Program to transfer samples via midi sample dump to/from Akai X7000 and S700 samplers. 
Although seemingly based on the SDS standard, Akai did not follow it exactly so the samplers 
needs to use a specific implementation using their protocol to work.

## What's currently working
* Transmitting sample file to the sampler, optionally with loop start/end

I can initiate a sample dump from the sampler and receive the information about the sample 
but it then seem's to quit after that. So that need further investigations.
Only tested on my X7000 with version 1.6 firmware.

The samplers does not support standard sampe rates like 44100 or 22050, but it will accept them, 
setting it to the nearest samplerate of it's supported ones and seemingly try to adjust the program 
tune so the sample should play in tune. However it does it incorrectly, correcting it in the opposite 
direction. So if sending a sample with sample rate 44100, it will set it to using a samplerate of 
39555.4 hz, and set the program tune to -88, but it should be +88 to give the correct pitch on playback.
It should be possible to send the program tune parameter with sysex, so it could be corrected.
