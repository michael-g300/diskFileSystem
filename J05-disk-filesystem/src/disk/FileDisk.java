package disk;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileDisk implements Disk {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private final Path m_diskFilePath;
    private final int m_numBlocks;
    private final int m_blockSize;
    private SeekableByteChannel m_channel;
    private boolean isShut = false;

    public FileDisk(final String filePath, final int numBlocks, final int blockSize) {
        m_numBlocks = numBlocks;
        m_blockSize = blockSize;
        m_diskFilePath = Paths.get(filePath);
        createDiskFile();
    }

    private void createDiskFile() {
        if (Files.exists(m_diskFilePath)) {
            if (Files.isDirectory(m_diskFilePath) || !Files.isReadable(m_diskFilePath) || !Files.isWritable(m_diskFilePath)) {
                System.out.println("Unable to read from file.");
                throw new IllegalArgumentException();
            }
        }
        else {
            try {
                Files.createFile(m_diskFilePath);
                System.out.println("New file created - " + m_diskFilePath.toString());
                m_channel = Files.newByteChannel(m_diskFilePath, StandardOpenOption.WRITE);
                for (int i = 0 ; i < m_numBlocks - 1 ; ++i) {
                    m_channel.position((long)i * m_blockSize);
                    ByteBuffer byteBuffer = ByteBuffer.wrap(createDefaultBlock());
                    m_channel.write(byteBuffer);
                }
                m_channel.close();
            }
            catch (IOException e) {
                System.out.println("File creation failed - " + m_diskFilePath);
                throw new RuntimeException(e);
            }
        }
    }

    private byte[] createDefaultBlock() {
        byte[] defaultBlock = new byte[m_blockSize];
        for (int i = 0 ; i < defaultBlock.length ; ++i) {
            defaultBlock[i] = 0;
        }
        return defaultBlock;
    }

    @Override
    public void read(final int blockNum, final byte[] buffer) {
        if (isShut) {
            System.out.println("Disk is shut down. Access denied.");
            throw new IllegalArgumentException();
        }
        if (buffer.length != m_blockSize) {
            System.out.println("Provided buffer of wrong size. Operation failed (read).");
            throw new IllegalArgumentException();
        }
        readLock.lock();
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(m_blockSize);
            m_channel = Files.newByteChannel(m_diskFilePath, StandardOpenOption.READ);
            m_channel.position((long)blockNum * m_blockSize);
            m_channel.read(byteBuffer);
            for (int i = 0 ; i < buffer.length ; ++i) {
                buffer[i] = byteBuffer.get(i);
            }
            m_channel.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public void write(int blockNum, byte[] buffer) {
        if (isShut) {
            System.out.println("Disk is shut down. Access denied.");
            throw new IllegalArgumentException();
        }
        if (buffer.length != m_blockSize) {
            System.out.println("byte input of wrong size. Operation failed (write).");
            throw new IllegalArgumentException();
        }
        writeLock.lock();
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            m_channel = Files.newByteChannel(m_diskFilePath, StandardOpenOption.WRITE);
            m_channel.position((long)blockNum * m_blockSize);
            m_channel.write(byteBuffer);
            m_channel.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            writeLock.unlock();
        }
    }

    public int shutdown() {
        if (isShut) {
            return 0;
        }
        isShut = true;
        return 1;
    }
}