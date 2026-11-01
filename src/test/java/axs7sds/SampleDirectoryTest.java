package axs7sds;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.prefs.Preferences;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SampleDirectoryTest
{
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  /** a node of its own, so the test doesn't change the directory the app remembers */
  private final Preferences prefs = Preferences.userRoot().node("axs7sds-test-" + System.nanoTime());

  @After
  public void removePrefs() throws Exception
  {
    prefs.removeNode();
  }

  @Test
  public void startsInDefaultDirectory()
  {
    assertEquals(new javax.swing.JFileChooser().getCurrentDirectory(),
        SampleDump.sampleFileChooser(prefs).getCurrentDirectory());
  }

  @Test
  public void startsInDirectoryOfLastFile() throws Exception
  {
    File directory = folder.newFolder("samples");
    SampleDump.rememberSampleDirectory(prefs, new File(directory, "sample.wav"));
    assertEquals(directory, SampleDump.sampleFileChooser(prefs).getCurrentDirectory());
  }

  @Test
  public void startsInNearestParentIfDirectoryIsGone() throws Exception
  {
    File directory = folder.newFolder("samples");
    SampleDump.rememberSampleDirectory(prefs, new File(directory, "gone/sample.wav"));
    assertEquals(directory, SampleDump.sampleFileChooser(prefs).getCurrentDirectory());
  }
}
