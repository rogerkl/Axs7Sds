package axs7sds.convert;

public class UnsupportedNumberOfChannelsException extends Exception
{
  private static final long serialVersionUID = -1945562710739313052L;

  public UnsupportedNumberOfChannelsException()
  {
    super();
  }

  public UnsupportedNumberOfChannelsException(final String message, final Throwable cause)
  {
    super(message, cause);
  }

  public UnsupportedNumberOfChannelsException(final String message)
  {
    super(message);
  }

  public UnsupportedNumberOfChannelsException(final Throwable cause)
  {
    super(cause);
  }
}
