package filesystem;

import java.nio.ByteBuffer;

public class Inode {
    private static final int DIRECT_POINTERS = 5;
    private static final int INODE_SIZE = 32;
    private static final int INTEGER_SIZE = 4;
    private static final int ELEMENTS_NUM = 8;

    private int m_valid = 0;
    private int m_size = 0;
    private int[] m_directPointers = new int[DIRECT_POINTERS];
    private int m_indirectPointer = 0;

    public Inode(final byte[] inodeData) {
        InodeParser(inodeData);
    }

    public Inode() {
        m_valid = 1;
    }

    public int Valid() {
        return m_valid;
    }

    public int Size() {
        return m_size;
    }

    public int[] Direct() {
        return m_directPointers;
    }

    public int Indirect() {
        return m_indirectPointer;
    }

    public void Vacant() {
        m_valid = 0;
        m_size = 0;
        m_directPointers = new int[DIRECT_POINTERS];
        m_indirectPointer = 0;
    }

    public void setSize(final int size) {
        m_size = size;
    }

    public void setDirectPointers(final int[] pointers) {
        m_directPointers = pointers;
    }

    public void setIndirectPointer(final int pointer) {
        m_indirectPointer = pointer;
    }

    public byte[] getBytes() {
        var byteBuffer = ByteBuffer.allocate(INODE_SIZE);
        byte[] inodeBytes = new byte[INODE_SIZE];
        byte[] valid = ByteBuffer.allocate(INTEGER_SIZE).putInt(m_valid).array();
        byte[] size = ByteBuffer.allocate(INTEGER_SIZE).putInt(m_size).array();
        byte[] direct0 = ByteBuffer.allocate(INTEGER_SIZE).putInt(m_directPointers[0]).array();
        byte[] direct1 = ByteBuffer.allocate(INTEGER_SIZE).putInt(m_directPointers[1]).array();
        byte[] direct2 = ByteBuffer.allocate(INTEGER_SIZE).putInt(m_directPointers[2]).array();
        byte[] direct3 = ByteBuffer.allocate(INTEGER_SIZE).putInt(m_directPointers[3]).array();
        byte[] direct4 = ByteBuffer.allocate(INTEGER_SIZE).putInt(m_directPointers[4]).array();
        byte[] indirect = ByteBuffer.allocate(INTEGER_SIZE).putInt(m_indirectPointer).array();
        for (int i = 0 ; i < ELEMENTS_NUM ; ++i) {
            if (i == 0) {
                for (int j = i * INTEGER_SIZE ; j < (i + 1) * INTEGER_SIZE ; ++j) {
                    inodeBytes[j] = valid[j - i * INTEGER_SIZE];
                }
            }
            else if (i == 1) {
                for (int j = i * INTEGER_SIZE ; j < (i + 1) * INTEGER_SIZE ; ++j) {
                    inodeBytes[j] = size[j - i * INTEGER_SIZE];
                }
            }
            else if (i == 2) {
                for (int j = i * INTEGER_SIZE ; j < (i + 1) * INTEGER_SIZE ; ++j) {
                    inodeBytes[j] = direct0[j - i * INTEGER_SIZE];
                }
            }
            else if (i == 3) {
                for (int j = i * INTEGER_SIZE ; j < (i + 1) * INTEGER_SIZE ; ++j) {
                    inodeBytes[j] = direct1[j - i * INTEGER_SIZE];
                }
            }
            else if (i == 4) {
                for (int j = i * INTEGER_SIZE ; j < (i + 1) * INTEGER_SIZE ; ++j) {
                    inodeBytes[j] = direct2[j - i * INTEGER_SIZE];
                }
            }
            else if (i == 5) {
                for (int j = i * INTEGER_SIZE ; j < (i + 1) * INTEGER_SIZE ; ++j) {
                    inodeBytes[j] = direct3[j - i * INTEGER_SIZE];
                }
            }
            else if (i == 6) {
                for (int j = i * INTEGER_SIZE ; j < (i + 1) * INTEGER_SIZE ; ++j) {
                    inodeBytes[j] = direct4[j - i * INTEGER_SIZE];
                }
            }
            else {
                for (int j = i * INTEGER_SIZE ; j < (i + 1) * INTEGER_SIZE ; ++j) {
                    inodeBytes[j] = indirect[j - i * INTEGER_SIZE];
                }
            }
        }
        return inodeBytes;
    }

    private void InodeParser(final byte[] inodeData) {
        if (inodeData.length != INODE_SIZE) {
            System.out.println("Byte array size incorrect. should be " + INODE_SIZE + " but actually is - " + inodeData.length);
            throw new IllegalArgumentException();
        }
        var byteBuffer = ByteBuffer.wrap(inodeData);
        m_valid = byteBuffer.getInt();
        m_size = byteBuffer.getInt();
        m_directPointers[0] = byteBuffer.getInt();
        m_directPointers[1] = byteBuffer.getInt();
        m_directPointers[2] = byteBuffer.getInt();
        m_directPointers[3] = byteBuffer.getInt();
        m_directPointers[4] = byteBuffer.getInt();
        m_indirectPointer = byteBuffer.getInt();
    }

}