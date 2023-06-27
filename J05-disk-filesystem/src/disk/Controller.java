package disk;

public interface Controller {
    public Disk get(final int diskNumber);
    public int shutdown();
    public int count();
}

