package disk;

import java.util.concurrent.ConcurrentHashMap;

public class DiskController implements Controller {
    private final int m_numBlocks;
    private final int m_blockSize;
    private static DiskController instance = null;
    private final String m_filesDirectory;
    private final ConcurrentHashMap<Integer, Disk> m_disksMap = new ConcurrentHashMap<>();
    private DiskController(final String filesDirectory, final int blocksNum, final int blockSize) {

        m_filesDirectory = filesDirectory;
        m_numBlocks = blocksNum;
        m_blockSize = blockSize;

    }

    public static DiskController getInstance(final String filesDirectory, final int blocksNum, final int blockSize) {
        if (instance == null) {
            instance = new DiskController(filesDirectory, blocksNum, blockSize);
        }
        return instance;
    }

    public Disk get(final int diskNumber) {
        final String fileName = "disk-" + getActualDiskNum(diskNumber) + ".dsk";
        var currentDisk = new FileDisk(m_filesDirectory + "/" + fileName, m_numBlocks, m_blockSize);
        System.out.println("disk acquired - " + m_filesDirectory + "/" + fileName);
        m_disksMap.put(diskNumber, currentDisk);
        return currentDisk;
    }

    public int count() {
        return m_disksMap.size();
    }

    public int shutdown() {
        int successfulShutdownCounter = 0;
        for (Disk disk : m_disksMap.values()) {
            successfulShutdownCounter += disk.shutdown();
        }
        return successfulShutdownCounter;
    }

    private String getActualDiskNum(int diskNumber) {
        if (diskNumber < 1) {
            System.out.println("Illegal number provided. Disk access denied.");
            throw new IllegalArgumentException();
        }
        if (diskNumber < 10) {
            System.out.println("disk number generated - " + "0" + diskNumber);
            return "0" + diskNumber;
        }
        return String.valueOf(diskNumber);
    }
}