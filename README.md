# Akai X7000/S700 sample dump
Program to transfer samples via midi sample dump to/from Akai X7000 and S700 samplers. 
Although seemingly based on the SDS standard, Akai did not follow it exactly so the samplers 
needs to use a specific implementation using their protocol to work.

**Note:** this is still experimental.

## What's currently working
* Transmitting sample file to the sampler, optionally with loop start/end
* Receiving a sample from the sampler, which can then be saved as a wav file
* Loop start, end and type (forward or alternating) are kept when transmitting, receiving, saving and loading

Only tested on my X7000 with version 1.6 firmware.

### Receiving
The sampler waits for an acknowledge after the sample header and after each block of sample data, but the
MIDI driver holds back incoming sysex data until its buffer is full or the end of the message arrives.
So the program can't see the header and blocks in time to answer them, and instead sends acknowledges at a
fixed pace, a little slower than it takes the sampler to send a block. Receiving a full sample takes about 30 seconds.

### Sample rates
The samplers play samples at a fixed set of sample rates a semitone apart, from 39555.4 Hz (16khz bandwidth)
down to 3924.38 Hz (1.6khz). Other sample rates like 44100 or 22050 are accepted, but the sampler rounds them
down to one of its rates and keeps the rest as program fine tune, and it applies that in the wrong direction
(sending 44100 Hz gives a tune of -88 cents, where +88 would be correct). The fine tune is also not part of a sample
dump, so a sample received back comes back at the rounded rate, up to a semitone flat. Rates more than a couple of
semitones above 39555.4 Hz (like 48000 Hz) come back with garbage as the sample rate.

To avoid this, a loaded file is resampled to the nearest of the sampler's rates (39555.4 Hz for anything above it).
The panel shows the rate it will be sent at and the rate it was resampled from.

## Building
The program is programmed in Java, minimum version Java 25.
You can start the build by running the gradle wrapper command.
Make sure that you path to the Java SDK is set up

    C:\Develop\Axs7Sds>set JAVA_HOME="C:\Develop\Java\jdk-25"
    C:\Develop\Axs7Sds>gradlew shadowJar

You will get a runnable jar file in build\libs\Axs7Sds-all.jar

## Running 
    C:\Develop\Axs7Sds>java -jar build\libs\Axs7Sds-all.jar

![Main window](images/MainWindow.png)

**Midi Device In:** Midi input port  
  
**Midi Device Out:Midi** output port  
  
*Since the protecol requires the sending of acknowledge packets, you will need to have a 2 way communication even if just sending data to the sampler (or just receiving)*  
  
**Sample Number:** Sample number to transmit or receive (1-6)  
  
**Normalize:** If checked normalize the audio to make sure all 12 bits are used (value = 1.)  
  
**Samples:** number of samples in file (after resampling) or received data, max 32752, if file is longer it will be truncated  
  
**Loop Start:** loop start
  
**Loop End:** loop end, by testing it seems like you should at least have the loop end point 10 samples shorter thn the end of the sample, or you will get a discontinuity even if the loop should be perfect. The sampler also uses the loop end as the end of the sample, anything after it is not kept.
  
**Loop type:** Forward or Alternating (back and forth). There is no one shot type, the sampler plays a sample as one shot when the loop is shorter than 5 samples, so for a one shot sample set loop start and end to the same point.
  
**X-fade length:** (experimental) if > 0 this will calculate a crossfade for the loop. This will be done on the wave data before sent by fading out the sample from loopend-xfadelength and fading in data from loopstart-xfadelength, so that means loop-start must be the same or later than the xfade-length. See the comment about the loop-end, same apply here, keep the loop end a little shorter than the sample length
  
**Load Sample:** read audio file to send to sampler. If the wav file has loop information (a smpl chunk), loop start, end and type are taken from its first loop, and a smpl chunk without loops gives a one shot sample. Without loop information the loop is set from the start to 100 samples before the end.  
  
**Save Sample:** save audio file received from sampler (or if you have read it from disk previous, it will write that, including truncation the length and doing the xfade calculation if set). The loop is saved in the wav file (smpl chunk), with no loop for a one shot sample.  
  
**Transmit:** send audio in memory to sampler  
  
**Receive:** receive sample from the sampler  
  
**Exit:** exit the program

## Testing with the sampler
`HardwareLoopback` sends test sines to the sampler, receives them back and checks that the data, pitch and loop
survive, at a few sample rates and with forward, alternating and one shot loops. The received samples are saved and
read back as wav files with their loop. It overwrites the given sample slot on the sampler. The sent and received
samples are kept in build/loopback.

    ./gradlew hardwareLoopback -Pdevice="XT [hw:0,0,0]" -Pslot=1
    ./gradlew hardwareLoopback -Pdevice="XT [hw:0,0,0]" -Pslot=1 -Ptests=rates -Prates=44100,22050 -Pdebug=true

`-Pdevice` is part of the MIDI device name, used for both input and output. `-Ptests` is `rates` or `loops` to
run only those tests.


