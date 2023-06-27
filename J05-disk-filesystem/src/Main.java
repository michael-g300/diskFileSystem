
import connectivity.Client;
import connectivity.FileServer;
import disk.DiskController;
import filesystem.FileSystem;

import java.io.IOException;

public class Main {
    private static final String DIR = "..";
    private final static int NUM_BLOCKS = 50;
    private final static int BLOCK_SIZE = 4096;

    public static void main(String[] args) {
        if (args.length == 1) {
            try {
                final FileServer server = new FileServer();
                server.run();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else if (args.length == 2) {
            try {
                final Client client = new Client();
                client.run();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

//        var controller = DiskController.getInstance(DIR, NUM_BLOCKS, BLOCK_SIZE);
//        var disk = controller.get(1);
//        var fileSystem = new FileSystem(disk);
//		String firstFileInput = "Hello. this is my first file";
//		var firstFile = fileSystem.newFile("firstFile");
//		firstFile.write(firstFileInput);
//
//		System.out.println("first file content : " + fileSystem.Open("firstFile").readString());
//
//		var secondFile = fileSystem.newFile("secondFile");
//		secondFile.write(8008185);

//        var firstFile = fileSystem.Open("firstFile");
//        firstFile.write("This is a rewrite", 7);
//        System.out.println("First file content at position 8 length 10: " + fileSystem.Open("firstFile").readString(8,10));
//        System.out.println("Second file content : " + fileSystem.Open("secondFile").readInt());

//        var thirdFile = fileSystem.newFile("thirdFile");
//        String thirdFileContent = "this is a file for deletion check.";
//        thirdFile.write(thirdFileContent);
//        var filesBeforDeletion = fileSystem.dir();
//        System.out.println("files currently in file system:");
//        for (String fileName : filesBeforDeletion) {
//            System.out.println(fileName);
//        }
//        fileSystem.Delete("thirdFile");
//
//        var files = fileSystem.dir();
//        System.out.println("files currently in file system:");
//        for (String fileName : files) {
//            System.out.println(fileName);
//        }
    }
}
