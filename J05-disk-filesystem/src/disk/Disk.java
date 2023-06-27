package disk;

public interface Disk {
    void read(int blockNum, byte[] buffer);
    void write(int blockNum, byte[] buffer);
    int shutdown();
}
