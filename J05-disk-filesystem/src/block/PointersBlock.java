package block;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class PointersBlock implements Block {
    private static final int BLOCK_SIZE = 4096;
    private static final int POINTER_SIZE = 4;

    private int[] m_inodePointers = new int[BLOCK_SIZE / POINTER_SIZE];

    public PointersBlock(final byte[] blockData) {
        PointersArrayParse(blockData);
    }

    private void PointersArrayParse(final byte[] blockData) {
        for (int i = 0 ; i < m_inodePointers.length ; ++i) {
            final byte[] currentPointerBytes = Arrays.copyOfRange(blockData, i * 4, (i + 1) * 4);
            final int currentPointer = ByteBuffer.wrap(currentPointerBytes).getInt();
            if (currentPointer == 0) {
                return;
            }
            m_inodePointers[i] = currentPointer;
        }
    }

    public int[] Pointers() {
        return m_inodePointers;
    }

    @Override
    public byte[] Data() {
        byte[] data = new byte[BLOCK_SIZE];
        for (int i = 0 ; i < m_inodePointers.length ; ++i) {
            byte[] currentPointer = ByteBuffer.allocate(POINTER_SIZE).putInt(m_inodePointers[i]).array();
            for (int j = i * POINTER_SIZE ; j < (i + 1) * POINTER_SIZE ; ++j) {
                data[j] = currentPointer[j - i * POINTER_SIZE];
            }
        }
        return data;
    }
}