package axs7sds.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.sound.midi.MidiDevice;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;

import axs7sds.gui.midi.MidiDeviceComboBoxModel;
import axs7sds.gui.midi.MidiDeviceComboBoxModel.MidiDeviceInfo;

import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.JCheckBox;

public class SampleDumpPanel extends JPanel
{
  private static final long         serialVersionUID        = 1L;
  private JComboBox<MidiDeviceInfo> cbMidiDeviceIn;
  private JTextField                fileName;
  private final DecimalFormat       numberInstanceNormalize = (DecimalFormat) NumberFormat.getNumberInstance();
  private final DecimalFormat       numberInstanceSamples   = (DecimalFormat) NumberFormat.getNumberInstance();

  public interface PanelOperations
  {
    void exit();

    void receive();

    void transmit();

    void saveSample();

    void loadSample();

  }

  public record SampleFileInfo(File sampleFile, int channels, float sampleRate, int sampleSizeInBits, int numSamples,
      String encoding)
  {
    @Override
    public String toString()
    {
      return (sampleFile == null ? "" : sampleFile.getName()) + ", ch=" + channels + ", sr=" + sampleRate
          + ", bits=" + sampleSizeInBits + ", numSamples=" + numSamples + ", encoding=" + encoding;
    }

    boolean valid()
    {
      return sampleRate > 0 && sampleSizeInBits > 0 && channels > 0;
    }
  }

  PanelOperations                   operations;
  private JButton                   btnLoadSample;
  private JButton                   btnSaveSample;
  private JButton                   btnTransmit;
  private JButton                   btnReceive;
  private JButton                   btnExit;
  private SampleFileInfo            sampleFileInfo;
  private JLabel                    lblMidiDeviceOut;
  private JComboBox<MidiDeviceInfo> cbMidiDeviceOut;
  private JSpinner                  spSampleNumber;
  private JTextArea                 taSampleInfo;
  private JFormattedTextField       edtNormalize;
  private JFormattedTextField       edtSamples;
  private JFormattedTextField       edtLoopStart;
  private JLabel                    lblLoopEnd;
  private JFormattedTextField       edtLoopEnd;
  private JLabel                    lblXfade;
  private JFormattedTextField       edtXFadeLength;
  private JCheckBox                 cbNormalize;

  /**
   * Create the panel.
   */
  public SampleDumpPanel()
  {
    numberInstanceSamples.setMaximumIntegerDigits(5);
    numberInstanceSamples.setMaximumFractionDigits(0);
    numberInstanceSamples.setParseIntegerOnly(true);

    numberInstanceNormalize.setMaximumIntegerDigits(1);
    numberInstanceNormalize.setMaximumFractionDigits(38);
    numberInstanceNormalize.setParseIntegerOnly(false);
    DecimalFormatSymbols decimalFormatSymbols = numberInstanceNormalize.getDecimalFormatSymbols();
    decimalFormatSymbols.setDecimalSeparator('.');
    decimalFormatSymbols.setGroupingSeparator(' ');
    numberInstanceNormalize.setDecimalFormatSymbols(decimalFormatSymbols);

    setLayout(new BorderLayout(0, 0));

    JPanel panel = new JPanel();
    add(panel, BorderLayout.NORTH);

    JPanel panel_1 = new JPanel();
    add(panel_1, BorderLayout.SOUTH);
    panel_1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

    btnLoadSample = new JButton("Load Sample");
    btnLoadSample.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        loadSample();
      }
    });
    panel_1.add(btnLoadSample);

    btnSaveSample = new JButton("Save Sample");
    btnSaveSample.setEnabled(false);
    btnSaveSample.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        saveSample();
      }
    });
    panel_1.add(btnSaveSample);

    btnTransmit = new JButton("Transmit");
    btnTransmit.setEnabled(false);
    btnTransmit.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        transmit();
      }
    });
    panel_1.add(btnTransmit);

    btnReceive = new JButton("Receive");
    btnReceive.setEnabled(false);
    btnReceive.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        receive();
      }
    });
    panel_1.add(btnReceive);

    btnExit = new JButton("Exit");
    btnExit.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        exit();
      }
    });
    panel_1.add(btnExit);

    JPanel panel_2 = new JPanel();
    add(panel_2, BorderLayout.WEST);

    JPanel panel_3 = new JPanel();
    add(panel_3, BorderLayout.EAST);

    JPanel panel_4 = new JPanel();
    add(panel_4, BorderLayout.CENTER);

    JLabel lblMidiDeviceIn = new JLabel("Midi Device In");

    cbMidiDeviceIn = new JComboBox<>(new MidiDeviceComboBoxModel(true));
    cbMidiDeviceIn.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        midiDeviceSelected();
      }
    });

    JLabel lblSample = new JLabel("Loaded Sample file");

    fileName = new JTextField();
    fileName.setEditable(false);
    fileName.setColumns(10);

    lblMidiDeviceOut = new JLabel("Midi Device Out");

    cbMidiDeviceOut = new JComboBox<>(new MidiDeviceComboBoxModel(false));
    cbMidiDeviceOut.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        midiDeviceSelected();
      }
    });

    spSampleNumber = new JSpinner();
    spSampleNumber.setModel(new SpinnerNumberModel(1, 1, 16, 1));

    JLabel lblSampleNumber = new JLabel("Sample Number");

    taSampleInfo = new JTextArea();

    JLabel lblSampleInfo = new JLabel("Sample Info");

    edtNormalize = new JFormattedTextField(numberInstanceNormalize);
    edtNormalize.setColumns(40);
    edtNormalize.setText("1.");

    cbNormalize = new JCheckBox("Normalize");
    cbNormalize.setSelected(true);

    JLabel lblSamples = new JLabel("Samples");

    edtSamples = new JFormattedTextField(numberInstanceSamples);
    edtSamples.setText("32752");
    edtSamples.setColumns(5);

    JLabel lblLoopStart = new JLabel("Loop Start");

    edtLoopStart = new JFormattedTextField(numberInstanceSamples);
    edtLoopStart.setColumns(5);

    lblLoopEnd = new JLabel("Loop End");

    edtLoopEnd = new JFormattedTextField(numberInstanceSamples);
    edtLoopEnd.setText("32752");
    edtLoopEnd.setColumns(5);

    lblXfade = new JLabel("X-fade length");

    edtXFadeLength = new JFormattedTextField(numberInstanceSamples);
    edtXFadeLength.setColumns(5);
    edtXFadeLength.setText("0");
    GroupLayout gl_panel_4 = new GroupLayout(panel_4);
    gl_panel_4.setHorizontalGroup(
        gl_panel_4.createParallelGroup(Alignment.TRAILING)
            .addGroup(gl_panel_4.createSequentialGroup()
                .addContainerGap()
                .addGroup(gl_panel_4.createParallelGroup(Alignment.LEADING, false)
                    .addComponent(lblMidiDeviceIn, GroupLayout.PREFERRED_SIZE, 91, GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblMidiDeviceOut, GroupLayout.PREFERRED_SIZE, 91, GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSampleNumber, GroupLayout.PREFERRED_SIZE, 91, GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSampleInfo, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblSample, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(gl_panel_4.createParallelGroup(Alignment.LEADING)
                    .addComponent(taSampleInfo, GroupLayout.DEFAULT_SIZE, 716, Short.MAX_VALUE)
                    .addGroup(gl_panel_4.createSequentialGroup()
                        .addComponent(spSampleNumber, GroupLayout.PREFERRED_SIZE, 48, GroupLayout.PREFERRED_SIZE)
                        .addGap(18)
                        .addComponent(cbNormalize)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(edtNormalize, GroupLayout.PREFERRED_SIZE, 48, GroupLayout.PREFERRED_SIZE)
                        .addGap(18)
                        .addComponent(lblSamples)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(edtSamples, GroupLayout.PREFERRED_SIZE, 48, GroupLayout.PREFERRED_SIZE)
                        .addGap(18)
                        .addComponent(lblLoopStart)
                        .addGap(10)
                        .addComponent(edtLoopStart, GroupLayout.PREFERRED_SIZE, 48, GroupLayout.PREFERRED_SIZE)
                        .addGap(18)
                        .addComponent(lblLoopEnd)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(edtLoopEnd, GroupLayout.PREFERRED_SIZE, 48, GroupLayout.PREFERRED_SIZE)
                        .addGap(18)
                        .addComponent(lblXfade)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(edtXFadeLength, GroupLayout.PREFERRED_SIZE, 48, GroupLayout.PREFERRED_SIZE))
                    .addComponent(cbMidiDeviceOut, 0, 716, Short.MAX_VALUE)
                    .addComponent(fileName, GroupLayout.DEFAULT_SIZE, 716, Short.MAX_VALUE)
                    .addComponent(cbMidiDeviceIn, 0, 716, Short.MAX_VALUE))
                .addContainerGap()));
    gl_panel_4.setVerticalGroup(
        gl_panel_4.createParallelGroup(Alignment.LEADING)
            .addGroup(gl_panel_4.createSequentialGroup()
                .addContainerGap()
                .addGroup(gl_panel_4.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblMidiDeviceIn)
                    .addComponent(cbMidiDeviceIn, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE))
                .addGap(8)
                .addGroup(gl_panel_4.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblMidiDeviceOut)
                    .addComponent(cbMidiDeviceOut, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(gl_panel_4.createParallelGroup(Alignment.BASELINE)
                    .addComponent(spSampleNumber, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSampleNumber)
                    .addComponent(cbNormalize)
                    .addComponent(edtNormalize, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSamples)
                    .addComponent(edtSamples, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                    .addComponent(edtLoopStart, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblLoopStart)
                    .addComponent(lblLoopEnd)
                    .addComponent(edtLoopEnd, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblXfade)
                    .addComponent(edtXFadeLength, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(gl_panel_4.createParallelGroup(Alignment.BASELINE)
                    .addComponent(fileName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSample))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(gl_panel_4.createParallelGroup(Alignment.BASELINE)
                    .addComponent(taSampleInfo, GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
                    .addComponent(lblSampleInfo))
                .addContainerGap()));
    panel_4.setLayout(gl_panel_4);

    edtLoopStart.addPropertyChangeListener(e -> {
      if ("value".equals(e.getPropertyName()))
      {
        int loopStart = getLoopStart();
        if (getXfadeLength() > loopStart)
        {
          SwingUtilities.invokeLater(() -> {
            setXfadeLength(loopStart);
          });
        }
      }
    });
    edtXFadeLength.addPropertyChangeListener(e -> {
      if ("value".equals(e.getPropertyName()))
      {
        int xfadeLength = getXfadeLength();
        if (xfadeLength > getLoopStart())
        {
          SwingUtilities.invokeLater(() -> {
            setLoopStart(xfadeLength);
          });
        }
      }
    });

  }

  protected void midiDeviceSelected()
  {
    enableTransmit();
    enableReceive();
  }

  protected void exit()
  {
    if (operations != null)
    {
      operations.exit();
    }

  }

  protected void receive()
  {
    if (operations != null)
    {
      operations.receive();
    }

  }

  protected void transmit()
  {
    if (operations != null)
    {
      operations.transmit();
    }
  }

  protected void saveSample()
  {
    if (operations != null)
    {
      operations.saveSample();
    }
  }

  protected void loadSample()
  {
    if (operations != null)
    {
      operations.loadSample();
    }
  }

  public void setOperations(PanelOperations operations)
  {
    this.operations = operations;
  }

  public void setSampleFile(SampleFileInfo sampleFileInfo)
  {
    this.sampleFileInfo = sampleFileInfo;
    if (sampleFileInfo != null && sampleFileInfo.sampleFile != null)
    {
      fileName.setText(sampleFileInfo.sampleFile().getPath());
    }
    else
    {
      fileName.setText("");
    }
    enableTransmit();
    enableSave();
    setSampleInfo(
        "sample rate: " + ((int) sampleFileInfo.sampleRate()) + "\nbit size:" + sampleFileInfo.sampleSizeInBits()
            + "\nlength (samples): " + sampleFileInfo.numSamples() + "\nencoding: " + sampleFileInfo.encoding());
  }

  private void enableSave()
  {
    btnSaveSample.setEnabled(sampleFileInfo != null && sampleFileInfo.valid());
  }

  private void enableTransmit()
  {
    btnTransmit
        .setEnabled(sampleFileInfo != null && sampleFileInfo.valid() && cbMidiDeviceIn.getSelectedItem() != null
            && cbMidiDeviceOut.getSelectedItem() != null);
  }

  private void enableReceive()
  {
    btnReceive.setEnabled(cbMidiDeviceIn.getSelectedItem() != null && cbMidiDeviceOut.getSelectedItem() != null);
  }

  public MidiDevice.Info getSelectedMidiDeviceIn()
  {
    return ((MidiDeviceInfo) cbMidiDeviceIn.getSelectedItem()).info();
  }

  public MidiDevice.Info getSelectedMidiDeviceOut()
  {
    return ((MidiDeviceInfo) cbMidiDeviceOut.getSelectedItem()).info();
  }

  public int getSampleNumber()
  {
    try
    {
      spSampleNumber.commitEdit();
    }
    catch (ParseException e)
    {
      throw new RuntimeException(e);
    }
    return (Integer) spSampleNumber.getValue() - 1;
  }

  public void setSampleInfo(String info)
  {
    taSampleInfo.setText(info);
  }

  public boolean normalize()
  {
    return cbNormalize.isSelected();
  }

  public double getNormalizeValue()
  {
    try
    {
      double doubleValue = numberInstanceNormalize.parse(edtNormalize.getText()).doubleValue();
      if (doubleValue > 1)
        return 1;
      return doubleValue;
    }
    catch (ParseException e)
    {
      e.printStackTrace();
      return 1.;
    }
  }

  public int getNumSamples()
  {
    try
    {
      if (edtSamples.getText().isBlank())
        return 32752;
      return numberInstanceSamples.parse(edtSamples.getText()).intValue();
    }
    catch (ParseException e)
    {
      throw new RuntimeException(e.getMessage());
    }
  }

  public int getLoopStart()
  {
    try
    {
      if (edtLoopStart.getText().isBlank())
        return 0;
      return numberInstanceSamples.parse(edtLoopStart.getText()).intValue();
    }
    catch (ParseException e)
    {
      throw new RuntimeException(e.getMessage());
    }
  }

  public int getLoopEnd()
  {
    try
    {
      if (edtLoopEnd.getText().isBlank())
        return 32752;
      return numberInstanceSamples.parse(edtLoopEnd.getText()).intValue();
    }
    catch (ParseException e)
    {
      throw new RuntimeException(e.getMessage());
    }
  }

  public int getXfadeLength()
  {
    try
    {
      if (edtXFadeLength.getText().isBlank())
        return 0;
      return numberInstanceSamples.parse(edtXFadeLength.getText()).intValue();
    }
    catch (ParseException e)
    {
      throw new RuntimeException(e.getMessage());
    }
  }

  public void setNumSamples(int numSamples)
  {
    edtSamples.setText(Integer.toString(numSamples));
  }

  public void setLoopStart(int loopStart)
  {
    edtLoopStart.setText(Integer.toString(loopStart));
  }

  public void setLoopEnd(int loopEnd)
  {
    edtLoopEnd.setText(Integer.toString(loopEnd));
  }

  public void setXfadeLength(int xfadeLength)
  {
    edtXFadeLength.setText(Integer.toString(xfadeLength));
  }
}
