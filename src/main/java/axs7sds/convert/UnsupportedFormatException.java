package axs7sds.convert;

public class UnsupportedFormatException extends Exception
{
  private static final long serialVersionUID = -7497311698202804342L;

  public UnsupportedFormatException()
  {
    super();
  }

  public UnsupportedFormatException(final String message, final Throwable cause)
  {
    super(message, cause);
  }

  public UnsupportedFormatException(final String message)
  {
    super(message);
  }

  public UnsupportedFormatException(final Throwable cause)
  {
    super(cause);
  }
}
