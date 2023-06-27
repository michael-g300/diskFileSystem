package block;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class DataBlock implements Block {
    private static final int INTEGER_SIZE = 4;

    private byte[] m_data;

    public DataBlock(final byte[] blockData) {
        m_data = blockData;
    }

    public int getInt(final int position) {
        byte[] intBytes = Arrays.copyOfRange(m_data, position, position + INTEGER_SIZE);
        return ByteBuffer.wrap(intBytes).getInt();
    }

    public String getString(final int startPosition, final int endPosition) {
        byte[] stringBytes = Arrays.copyOfRange(m_data, startPosition, endPosition);
        return new String(stringBytes);
    }

    public byte[] getBytes(final int position, final int length) {
        return Arrays.copyOfRange(m_data, position, position + length);
    }

    @Override
    public byte[] Data() {
        return m_data;
    }

    public void Update(final byte[] data, final int position) {
        if (position + data.length >= m_data.length) {
            System.out.println("cannot write " + data.length + " bytes into block from position number " + position);
            throw new IllegalArgumentException();
        }
        for (int i = position ; i < data.length ; ++i) {
            m_data[i] = data[i - position];
        }
    }
}