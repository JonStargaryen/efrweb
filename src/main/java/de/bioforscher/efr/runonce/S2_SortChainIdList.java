package de.bioforscher.efr.runonce;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class S2_SortChainIdList {
    public static void main(String[] args) throws IOException {
        Files.lines(Paths.get("/home/bittrich/chainids.dat"))
                .sorted()
                .skip(300000)
                .limit(100000)
                .forEach(System.out::println);
    }
}
