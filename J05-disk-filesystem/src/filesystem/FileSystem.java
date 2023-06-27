package filesystem;

import java.nio.ByteBuffer;
import java.util.*;

import block.PointersBlock;
import block.SuperBlock;
import disk.Disk;

public class FileSystem {
    private final static int MAGIC_NUMBER = 1994;
    private final static int NUM_BLOCKS = 50;
    private final static int INODE_BLOCKS_PERCENTAGE = 10;
    private final static int BLOCK_SIZE = 4096;
    private static final int INODE_SIZE = 32;
    private static final int INODES_PER_BLOCK = BLOCK_SIZE / INODE_SIZE;
    private static final char FILE_NAME_SEPARATOR = ':';
    private static final char FILE_INUMBER_SEPARATOR = '%';
    private static final String META_FILE_NAME = "metaFile";
    private final Disk m_disk;
    private SuperBlock m_superBlock;
    private final Inode[] m_inodes = new Inode[(NUM_BLOCKS / (100 / INODE_BLOCKS_PERCENTAGE)  + (NUM_BLOCKS % (100 / INODE_BLOCKS_PERCENTAGE) == 0 ? 0 : 1)) * BLOCK_SIZE / INODE_SIZE];
    private final int[] m_blocks = new int[NUM_BLOCKS];
    private final Map<String, Integer> m_files = new LinkedHashMap<>();

    public FileSystem(final Disk disk) {
        m_disk = disk;
        getSuperBlock();
        getMetaData();
    }

    private void getMetaData() {
        Inode metaInode = m_inodes[m_files.get(META_FILE_NAME)];
        if (metaInode.Size() == 0) {
            System.out.println("metadata Inode is empty.");
            return;
        }
        var metaFile = this.Open(META_FILE_NAME);
        byte[] metadata = metaFile.read();
        System.out.println("metadata content: " + new String(metadata) + " , inode number: " + m_files.get(META_FILE_NAME));
        metadataParse(metadata);
    }

    private void metadataParse(final byte[] metadata) {
        for (int i = 0 ; i < metadata.length ; ++i) {
            final int fileNameStart = i;
            int fileNameEnd = fileNameStart;
            while ((char)metadata[i] != FILE_NAME_SEPARATOR) {
                ++ fileNameEnd;
                ++ i;
            }
            final String fileName = new String(Arrays.copyOfRange(metadata, fileNameStart, fileNameEnd));
            ++ i;
            final int fileInodeNumStart = i;
            int fileInodeNumEnd = fileInodeNumStart;
            while ((char)metadata[i] != FILE_INUMBER_SEPARATOR) {
                ++ fileInodeNumEnd;
                ++ i;
            }
            int fileInodeNum;
            if (fileInodeNumEnd - fileInodeNumStart == 1) {
                String numString = Byte.toString(metadata[fileInodeNumStart]);
                fileInodeNum = Integer.parseInt(numString);
            }
            else {
                fileInodeNum = ByteBuffer.wrap(Arrays.copyOfRange(metadata, fileInodeNumStart, fileInodeNumEnd)).getInt();
            }
            byte[] inodeData = getInodeDataFromDisk(fileInodeNum);
            Inode fileInode = new Inode(inodeData);
            for (int j = 0 ; j < fileInode.Direct().length ; ++j) {
                m_blocks[fileInode.Direct()[j]] = 1;
            }
            m_inodes[fileInodeNum] = fileInode;
            System.out.println("current file inode " + fileInodeNum + " has direct pointer to block number " + fileInode.Direct()[0]);
            m_files.put(fileName, fileInodeNum);
            System.out.println("file " + fileName + " loaded from existing disk, with inode number - " + fileInodeNum);
        }
    }

    private byte[] getInodeDataFromDisk(final int fileInodeNum) {
        final int blockNum = fileInodeNum / INODES_PER_BLOCK + 1;
        final int position = (fileInodeNum % INODES_PER_BLOCK) * INODE_SIZE;
        byte[] inodeBlockData = new byte[BLOCK_SIZE];
        m_disk.read(blockNum, inodeBlockData);
        byte[] inodeData = new byte[INODE_SIZE];
        for (int i = position ; i < position + INODE_SIZE ; ++i) {
            inodeData[i - position] = inodeBlockData[i];
        }
        return inodeData;
    }

    private void getSuperBlock() {
        byte[] superBlockData = new byte[BLOCK_SIZE];
        m_disk.read(0, superBlockData);
        var superBlock = new SuperBlock(superBlockData);
        if (superBlock.MagicNumber() != MAGIC_NUMBER) {
            format();
            getSuperBlock();
        }
        else {
            m_superBlock = superBlock;
            mount();
        }
    }

    private void format() {
        m_superBlock = new SuperBlock(MAGIC_NUMBER, NUM_BLOCKS);
        m_disk.write(0, m_superBlock.Data());
        m_blocks[0] = 1;
        var metaFile = new SimpleFile(META_FILE_NAME, 0, this);
        m_inodes[0] = new Inode();
        int blockInUse = updateInodeOnDisk(0, m_inodes[0].getBytes());
        m_blocks[blockInUse] = 1;
        m_files.put(metaFile.name(), 0);
    }

    private void mount() {
        m_blocks[0] = 1;
        m_blocks[1] = 1;
        Inode metaInode = new Inode(getInodeDataFromDisk(0));
        m_inodes[0] = metaInode;
        m_files.put(META_FILE_NAME, 0);
    }

    private int updateInodeOnDisk(final int inodeNum, final byte[] inodeData) {
        final int blockNum = inodeNum / INODES_PER_BLOCK + 1;
        final int position = (inodeNum % INODES_PER_BLOCK) * INODE_SIZE;
        System.out.println("updating inode " + inodeNum + " in block number " + blockNum + " at position " + position);
        byte[] inodeBlockData = new byte[BLOCK_SIZE];
        m_disk.read(blockNum, inodeBlockData);
        for (int i = position ; i < position + INODE_SIZE ; ++i) {
            inodeBlockData[i] = inodeData[i - position];
        }
        m_disk.write(blockNum, inodeBlockData);
        return blockNum;
    }

    public SimpleFile Open(final String fileName) {
        if (!m_files.containsKey(fileName)) {
            System.out.println("No matching files in system.");
            return null;
        }
        return new SimpleFile(fileName, m_files.get(fileName), this);
    }
    private void addToMetaFile(final String fileName, final int inodeNum) {
        var metaFile = this.Open(META_FILE_NAME);
        metaFile.write(fileName);
        metaFile.write(":");
        metaFile.write(inodeNum);
        metaFile.write("%");
    }
    private void updateMetaFile() {
        var metaFile = this.Open(META_FILE_NAME);
        metaFile.clear();
        Iterator<String> itr = m_files.keySet().iterator();
        while (itr.hasNext()) {
            final String fileName = itr.next();
            final int fileInodeNum = m_files.get(fileName);
            metaFile.write(fileName);
            metaFile.write(":");
            metaFile.write(fileInodeNum);
            metaFile.write("%");
        }
    }
    private int findFreeInode() {
        for (int i = 0 ; i < m_inodes.length ; ++i) {
            if (m_inodes[i] == null || m_inodes[i].Valid() == 0) {
                return i;
            }
        }
        return -1;
    }
    boolean updateFileAtPosition(final int inodeNum, final byte[] newData, final int position) {
        var fileInode = m_inodes[inodeNum];
        if (position < 0 || position > fileInode.Size()) {
            System.out.println("Invalid position to write in. File update failed");
            return false;
        }
        int currentPosition = position;
        int newDataPosition = 0;
        while (currentPosition < fileInode.Size() && newDataPosition < newData.length) {
            final int currentBlockSerialNum = position / BLOCK_SIZE + (position % BLOCK_SIZE == 0 ? 0 : 1);
            if (currentBlockSerialNum < fileInode.Direct().length) {
                final int currentBlockNum = fileInode.Direct()[currentBlockSerialNum - 1];
                if (currentBlockNum == 0) {
                    break;
                }
                newDataPosition = updateBlockAtPosition(fileInode, currentBlockNum, currentBlockSerialNum, currentPosition, newData, newDataPosition);
                currentPosition = position + newDataPosition;
            }
            else {
                byte[] pointersBlockData = new byte[BLOCK_SIZE];
                m_disk.read(fileInode.Indirect(), pointersBlockData);
                var pointersBlock = new PointersBlock(pointersBlockData);
                final int currentBlockNum = pointersBlock.Pointers()[currentBlockSerialNum - 1];
                if (currentBlockNum == 0) {
                    break;
                }
                newDataPosition = updateBlockAtPosition(fileInode, currentBlockNum, currentBlockSerialNum, currentPosition, newData, newDataPosition);
                currentPosition = position + newDataPosition;
            }
        }
        if (newDataPosition < newData.length) {
            final byte[] remainingData = Arrays.copyOfRange(newData, newDataPosition, newData.length);
            return updateFileOnDisk(inodeNum, remainingData);
        }
        else {
            return true;
        }
    }

    private int updateBlockAtPosition(final Inode fileInode, final int currentBlockNum, final int currentBlockSerialNum, int position, final byte[] newData, int newDataPosition) {
        byte[] currentBlockData = new byte[BLOCK_SIZE];
        m_disk.read(currentBlockNum, currentBlockData);
        while (newDataPosition < newData.length) {
            if (position >= currentBlockSerialNum * BLOCK_SIZE || position == fileInode.Size()) {
                break;
            }
            currentBlockData[position % BLOCK_SIZE] = newData[newDataPosition];
            ++position;
            ++newDataPosition;
        }
        m_disk.write(currentBlockNum, currentBlockData);
        return newDataPosition;
    }
    boolean updateFileOnDisk(final int inodeNum, final byte[] newData) {
        final int lastBlockNum = findFileDataLastBlock(inodeNum);
        byte[] lastBlockData = new byte[BLOCK_SIZE];
        if (lastBlockNum != 0) {
            m_disk.read(lastBlockNum, lastBlockData);
        }
        var fileInode = m_inodes[inodeNum];
        final int writtenDataSize = fileInode.Size() % BLOCK_SIZE;
        final boolean isLastBlockFull = writtenDataSize == 0 && fileInode.Size() != 0;
        int position = 0;
        for (int i = 0 ; i < fileInode.Direct().length ; ++i) {
            if (fileInode.Direct()[i] == 0) {
                final int newBlockNum = findFreeBlock();
                byte[] newBlockData = new byte[BLOCK_SIZE];
                int j;
                for (j = 0 ; j < (Math.min(newData.length - position, BLOCK_SIZE)) ; ++j) {
                    newBlockData[j] = newData[j + position];
                }
                position += j;
                fileInode.Direct()[i] = newBlockNum;
                m_blocks[newBlockNum] = 1;
                m_disk.write(newBlockNum, newBlockData);
            }
            if (fileInode.Direct()[i] == lastBlockNum && !isLastBlockFull) {
                int j;
                for (j = 0 ; j < (Math.min(newData.length, BLOCK_SIZE - writtenDataSize)) ; ++j) {
                    lastBlockData[writtenDataSize + j] = newData[position + j];
                }
                position += j;
                m_disk.write(lastBlockNum, lastBlockData);
            }
            if (position == newData.length) {
                break;
            }
        }
        if (position != newData.length) {
            byte[] pointersBlockData = new byte[BLOCK_SIZE];
            if (fileInode.Indirect() == 0) {
                final int nextFreeBlock = findFreeBlock();
                m_blocks[nextFreeBlock] = 1;
                fileInode.setIndirectPointer(nextFreeBlock);
            }
            m_disk.read(fileInode.Indirect(), pointersBlockData);
            final PointersBlock pointersBlock = new PointersBlock(pointersBlockData);
            int[] indirectBlocks = pointersBlock.Pointers();
            for (int i = 0 ; i < indirectBlocks.length ; ++i) {
                if (indirectBlocks[i] == 0) {
                    final int newBlockNum = findFreeBlock();
                    byte[] newBlockData = new byte[BLOCK_SIZE];
                    int j;
                    for (j = 0 ; j < (Math.min(newData.length - position, BLOCK_SIZE)) ; ++j) {
                        newBlockData[j] = newData[j + position];
                    }
                    position += j;
                    indirectBlocks[i] = newBlockNum;
                    m_blocks[newBlockNum] = 1;
                    m_disk.write(newBlockNum, newBlockData);
                }
                if (indirectBlocks[i] + 5 == lastBlockNum && !isLastBlockFull) {
                    int j;
                    for (j = 0 ; j < (Math.min(newData.length, BLOCK_SIZE - writtenDataSize)) ; ++j) {
                        lastBlockData[writtenDataSize + 1 + j] = newData[position];
                    }
                    position += j;
                    m_disk.write(lastBlockNum, lastBlockData);
                }
                if (position == newData.length) {
                    pointersBlockData = pointersBlock.Data();
                    m_disk.write(fileInode.Indirect(), pointersBlockData);
                    break;
                }
            }
        }
        fileInode.setSize(fileInode.Size() + newData.length);
        m_inodes[inodeNum] = fileInode;
        updateInodeOnDisk(inodeNum, fileInode.getBytes());
        return true;
    }

    private int findFileDataLastBlock(final int inodeNum) {
        final Inode fileInode = m_inodes[inodeNum];
        final int blocksNum = fileInode.Size() / BLOCK_SIZE + (fileInode.Size() % BLOCK_SIZE == 0 ? 0 : 1);
        if (blocksNum == 0) {
            return 0;
        }
        if (blocksNum <= 5) {
            return fileInode.Direct()[blocksNum - 1];
        }
        else {
            final int inirectPointerNum = blocksNum - 5;
            byte[] pointersBlockData = new byte[BLOCK_SIZE];
            m_disk.read(fileInode.Indirect(), pointersBlockData);
            final PointersBlock pointersBlock = new PointersBlock(pointersBlockData);
            final int[] indirectPointers = pointersBlock.Pointers();
            return indirectPointers[inirectPointerNum - 1];
        }
    }
    public byte[] getFileDataAtPosition(final int inodeNum, int position, final int size) {
        final Inode fileInode = m_inodes[inodeNum];
        byte[] output = new byte[size];
        if (position < 0 || position >= fileInode.Size() || position + size > fileInode.Size() || size <= 0) {
            System.out.println("Unable to read at position " + position + " , data size of - " + size);
            return output;
        }
        int writingPosition = 0;
        while (writingPosition < size) {
            final int currentBlockSerialNum = position / BLOCK_SIZE + (position % BLOCK_SIZE == 0 ? 0 : 1);
            byte[] currentBlockData = new byte[BLOCK_SIZE];
            if (currentBlockSerialNum < fileInode.Direct().length) {
                m_disk.read(fileInode.Direct()[currentBlockSerialNum - 1], currentBlockData);
            }
            else {
                byte[] pointersBlockData = new byte[BLOCK_SIZE];
                m_disk.read(fileInode.Indirect(), pointersBlockData);
                var pointersBlock = new PointersBlock(pointersBlockData);
                final int currentBlockNum = pointersBlock.Pointers()[currentBlockSerialNum - 1];
                m_disk.read(currentBlockNum, currentBlockData);
            }
            for (int i = position % BLOCK_SIZE ; i < BLOCK_SIZE ; ++i) {
                if (writingPosition == size) {
                    break;
                }
                output[writingPosition] = currentBlockData[i];
                ++writingPosition;
            }
        }
        return output;
    }
    byte[] getFileData(final int inodeNum) {
        final Inode fileInode = m_inodes[inodeNum];
        byte[] fileData = new byte[fileInode.Size()];
        final int blocksNum = fileInode.Size() / BLOCK_SIZE + (fileInode.Size() % BLOCK_SIZE == 0 ? 0 : 1);
        for (int i = 0 ; i < blocksNum ; ++i) {
            byte[] currentBlockData = new byte[BLOCK_SIZE];
            if (fileInode.Direct()[i] == 0) {
                System.out.println("missing file data location on disk. Incomplete data received.");
                return fileData;
            }
            if (fileInode.Direct()[i] != 0) {
                m_disk.read(fileInode.Direct()[i], currentBlockData);
                if (i == blocksNum - 1) {
                    final int currentDataLength = fileInode.Size() % BLOCK_SIZE == 0 ? BLOCK_SIZE : fileInode.Size() % BLOCK_SIZE;
                    for (int k = 0 ; k < currentDataLength ; ++k) {
                        fileData[(blocksNum - 1) * BLOCK_SIZE + k] = currentBlockData[k];
                    }
                    return fileData;
                }
                System.arraycopy(currentBlockData, 0, fileData, i * BLOCK_SIZE, BLOCK_SIZE);
            }
            if (blocksNum > fileInode.Direct().length) {
                byte[] pointersBlockData = new byte[BLOCK_SIZE];
                m_disk.read(fileInode.Indirect(), pointersBlockData);
                var pointersBlock = new PointersBlock(pointersBlockData);
                int[] pointers = pointersBlock.Pointers();
                for (int j = 0 ; j < pointers.length ; ++j) {
                    m_disk.read(pointers[j], currentBlockData);
                    if (j + fileInode.Direct().length == blocksNum - 1) {
                        final int currentDataLength = fileInode.Size() % BLOCK_SIZE == 0 ? BLOCK_SIZE : fileInode.Size() % BLOCK_SIZE;
                        for (int k = 0 ; k < currentDataLength ; ++k) {
                            fileData[(blocksNum - 1) * BLOCK_SIZE + k] = currentBlockData[k];
                        }
                        return fileData;
                    }
                    System.arraycopy(currentBlockData, 0, fileData, (i + j) * BLOCK_SIZE, BLOCK_SIZE);
                }
            }
        }

        return fileData;
    }
    void clearFileData(final int inodeNum) {
        final Inode fileInode = m_inodes[inodeNum];
        byte[] emptyBlock = new byte[BLOCK_SIZE];
        for (int i = 0 ; i < fileInode.Direct().length ; ++i) {
            if (fileInode.Direct()[i] == 0) {
                break;
            }
            m_disk.write(fileInode.Direct()[i], emptyBlock);
            m_blocks[fileInode.Direct()[i]] = 0;
        }
        if (fileInode.Indirect() != 0) {
            byte[] pointersBlockData = new byte[BLOCK_SIZE];
            m_disk.read(fileInode.Indirect(), pointersBlockData);
            var pointersBlock = new PointersBlock(pointersBlockData);
            int[] pointers = pointersBlock.Pointers();
            for (int i = 0 ; i < pointers.length ; ++i) {
                if (pointers[i] == 0) {
                    break;
                }
                m_disk.write(pointers[i], emptyBlock);
                m_blocks[pointers[i]] = 0;
            }
            m_disk.write(fileInode.Indirect(), new byte[BLOCK_SIZE]);
            m_blocks[fileInode.Indirect()] = 0;
            fileInode.setIndirectPointer(0);
        }
        fileInode.setSize(0);
        fileInode.setDirectPointers(new int[5]);
        m_inodes[inodeNum] = fileInode;
        updateInodeOnDisk(inodeNum, fileInode.getBytes());
    }
    private int findFreeBlock() {
        for (int i = 1 ; i < m_blocks.length ; ++i) {
            if (m_blocks[i] == 0) {
                return i;
            }
        }
        return -1;
    }
    public SimpleFile newFile(final String fileName) {
        int inodeNum = findFreeInode();
        if (inodeNum == -1) {
            System.out.println("No free Inodes left. File creation failed");
            return null;
        }
        m_inodes[inodeNum] = new Inode();
        m_files.put(fileName, inodeNum);
        addToMetaFile(fileName, inodeNum);
        return new SimpleFile(fileName, inodeNum, this);
    }
    public List<String> dir() {
        var fileNamesList = new ArrayList<String>(m_files.size());
        for (String filename : m_files.keySet()) {
            fileNamesList.add(filename);
        }
        return fileNamesList;
    }
    public boolean Delete(final String fileName) {
        if (!m_files.containsKey(fileName)) {
            System.out.println("The file " + fileName + " cannot be located on current disk. Deletion failed.");
            return false;
        }
        clearFileData(m_files.get(fileName));
        m_inodes[m_files.get(fileName)] = null;
        m_files.remove(fileName);
        updateMetaFile();
        return true;
    }
    public int getfileSize(final String fileName) {
        if (!m_files.containsKey(fileName)) {
            return -1;
        }
        final Inode fileInode = m_inodes[m_files.get(fileName)];
        return fileInode.Size();
    }
}