package filesystem;

import java.nio.ByteBuffer;

public class SimpleFile {
    private static final int INTEGER_SIZE = 4;

    private final int m_inodeNum;
    private final String m_name;
    private final FileSystem m_fileSystem;
    private int m_position;

    SimpleFile(final String name, final int inodeNum, final FileSystem fileSystem) {
        m_inodeNum = inodeNum;
        m_name = name;
        m_fileSystem = fileSystem;
    }
    public String name() {
        return m_name;
    }

    public void write(final byte[] newData) {
        m_fileSystem.updateFileOnDisk(m_inodeNum, newData);
    }
    public void write(final byte[] newData, final int position) {
        m_fileSystem.updateFileAtPosition(m_inodeNum, newData, position);
    }

    public void write(final int num) {
        byte[] numBytes = ByteBuffer.allocate(INTEGER_SIZE).putInt(num).array();
        write(numBytes);
    }
    public void write(final int num, final int position) {
        byte[] numBytes = ByteBuffer.allocate(INTEGER_SIZE).putInt(num).array();
        write(numBytes, position);
    }

    public void write(final String string) {
        byte[] stringBytes = string.getBytes();
        write(stringBytes);
    }
    public void write(final String string, final int position) {
        byte[] stringBytes = string.getBytes();
        write(stringBytes, position);
    }

    public byte[] read() {
        return m_fileSystem.getFileData(m_inodeNum);
    }
    public byte[] read(final int position, final int size) {
        return m_fileSystem.getFileDataAtPosition(m_inodeNum, position, size);
    }

    public int readInt() {
        return ByteBuffer.wrap(read()).getInt();
    }
    public int readInt(final int position, final int size) {

        return ByteBuffer.wrap(read(position, size)).getInt();
    }

    public String readString() {
        return new String(read());
    }
    public String readString(final int position, final int size) {
        return new String(read(position, size));
    }

    public void clear() {
        m_fileSystem.clearFileData(m_inodeNum);
    }
}