package block;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class SuperBlock implements Block {
    private static final int ELEMENTS_NUM = 4;
    private static final int INTEGER_SIZE = 4;
    private static final int INODE_BLOCK_PERCENTAGE = 10;
    private static final int INODE_SIZE = 32;
    private static final int BLOCK_SIZE = 4096;

    private final int m_magicNum;
    private final int m_blocks;
    private final int m_inodeBlocks;
    private final int m_inodes;

    public SuperBlock(final int magicNum, final int blocks) {
        m_magicNum = magicNum;
        m_blocks = blocks;
        m_inodeBlocks = m_blocks / (100 / INODE_BLOCK_PERCENTAGE) + (m_blocks % (100 / INODE_BLOCK_PERCENTAGE) == 0 ? 0 : 1);
        m_inodes = m_inodeBlocks * (BLOCK_SIZE / INODE_SIZE);
    }

    public SuperBlock(final byte[] data) {
        if (data.length != BLOCK_SIZE) {
            System.out.println("Byte array size incorrect. should be " + INODE_SIZE + " but actually is - " + data.length);
            throw new IllegalArgumentException();
        }
        byte[] magicNum = Arrays.copyOfRange(data, 0, 4);
        m_magicNum = ByteBuffer.wrap(magicNum).getInt();

        byte[] blocks = Arrays.copyOfRange(data, 4, 8);
        m_blocks = ByteBuffer.wrap(blocks).getInt();

        byte[] inodeBlocks = Arrays.copyOfRange(data, 8, 12);
        m_inodeBlocks = ByteBuffer.wrap(inodeBlocks).getInt();

        byte[] inodes = Arrays.copyOfRange(data, 12, 16);
        m_inodes = ByteBuffer.wrap(inodes).getInt();
    }

    public int MagicNumber() {
        return m_magicNum;
    }

    public int Blocks() {
        return m_blocks;
    }

    public int InodeBlocks() {
        return m_inodeBlocks;
    }

    public int Inodes() {
        return m_inodes;
    }

    @Override
    public byte[] Data() {
        byte[] data = new byte[BLOCK_SIZE];
        byte[] magicNum = ByteBuffer.allocate(INTEGER_SIZE).putInt(m_magicNum).array();
        byte[] blocks = ByteBuffer.allocate(INTEGER_SIZE).putInt(m_blocks).array();
        byte[] iblocks = ByteBuffer.allocate(INTEGER_SIZE).putInt(m_inodeBlocks).array();
        byte[] inodes = ByteBuffer.allocate(INTEGER_SIZE).putInt(m_inodes).array();
        for (int i = 0 ; i < ELEMENTS_NUM ; ++i) {
            if (i == 0) {
                for (int j = i * INTEGER_SIZE ; j < (i + 1) * INTEGER_SIZE ; ++j) {
                    data[j] = magicNum[j - i * INTEGER_SIZE];
                }
            }
            if (i == 1) {
                for (int j = i * INTEGER_SIZE ; j < (i + 1) * INTEGER_SIZE ; ++j) {
                    data[j] = blocks[j - i * INTEGER_SIZE];
                }
            }
            if (i == 2) {
                for (int j = i * INTEGER_SIZE ; j < (i + 1) * INTEGER_SIZE ; ++j) {
                    data[j] = iblocks[j - i * INTEGER_SIZE];
                }
            }
            if (i == 3) {
                for (int j = i * INTEGER_SIZE ; j < (i + 1) * INTEGER_SIZE ; ++j) {
                    data[j] = inodes[j - i * INTEGER_SIZE];
                }
            }
        }
        return data;
    }
}