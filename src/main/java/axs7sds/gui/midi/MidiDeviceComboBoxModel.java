package axs7sds.gui.midi;

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import axs7sds.gui.midi.MidiDeviceComboBoxModel.MidiDeviceInfo;

@SuppressWarnings("serial")
public class MidiDeviceComboBoxModel extends AbstractListModel<MidiDeviceInfo>
    implements
      ComboBoxModel<MidiDeviceInfo>
{

  public record MidiDeviceInfo(String name, MidiDevice.Info info)
  {
    @Override
    public final String toString()
    {
      return name;
    }
  }

  List<MidiDeviceInfo> devices  = new ArrayList<>();
  MidiDeviceInfo       selected = null;

  public MidiDeviceComboBoxModel(boolean in)
  {
    MidiDevice.Info[] deviceInfos = MidiSystem.getMidiDeviceInfo();
    for (Info info : deviceInfos)
    {
      try
      {
        if (!in && MidiSystem.getMidiDevice(info).getMaxReceivers() != 0)
        {
          devices.add(new MidiDeviceInfo(info.getName(), info));
        }
        else if (in && MidiSystem.getMidiDevice(info).getMaxTransmitters() != 0)
        {
          devices.add(new MidiDeviceInfo(info.getName(), info));
        }
      }
      catch (MidiUnavailableException e)
      {
        e.printStackTrace();
      }
    }
  }

  @Override
  public int getSize()
  {
    return devices.size();
  }

  @Override
  public MidiDeviceInfo getElementAt(int index)
  {
    return devices.get(index);
  }

  @Override
  public void setSelectedItem(Object anItem)
  {
    selected = (MidiDeviceInfo) anItem;
  }

  @Override
  public Object getSelectedItem()
  {
    return selected;
  }
}
