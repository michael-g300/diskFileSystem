package block;

import java.util.Arrays;

import filesystem.Inode;

public class InodeBlock implements Block {
    private static final int BLOCK_SIZE = 4096;
    private static final int INODE_SIZE = 32;

    private Inode[] m_inodes = new Inode[BLOCK_SIZE / INODE_SIZE];

    public InodeBlock(final byte[] blockData) {
        InodeArrayParse(blockData);
    }

    public Inode[] Inodes() {
        return m_inodes;
    }

    public void setInodes(final Inode[] inodes) {
        m_inodes = inodes;
    }

    @Override
    public byte[] Data() {
        byte[] data = new byte[BLOCK_SIZE];
        for (int i = 0 ; i < m_inodes.length ; ++i) {
            byte[] currentInode = m_inodes[i].getBytes();
            for (int j = i * INODE_SIZE ; j < (i + 1) * INODE_SIZE ; ++j) {
                data[j] = currentInode[j - i * INODE_SIZE];
            }
        }
        return data;
    }

    private void InodeArrayParse(final byte[] blockData) {
        for (int i = 0 ; i < m_inodes.length ; ++i) {
            byte[] currentInodeData = Arrays.copyOfRange(blockData, i + INODE_SIZE, (i + 1) * INODE_SIZE);
            m_inodes[i] = new Inode(currentInodeData);
        }
    }
}