package de.bioforscher.efr.runonce;

import de.bioforscher.jstructure.model.identifier.ChainIdentifier;
import de.bioforscher.jstructure.model.structure.Chain;
import de.bioforscher.jstructure.model.structure.Structure;
import de.bioforscher.jstructure.model.structure.StructureParser;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

public class S1_CreateChainIdList {
    public static void main(String[] args) throws IOException {
        Path pdbDirectory = Paths.get("/var/local/pdb/");
        FileWriter fileWriter = new FileWriter("/home/bittrich/chainids.dat");
        int[] counter = new int[1];

        Files.walk(pdbDirectory)
                .filter(path -> !Files.isDirectory(path))
                .filter(path -> path.toFile().getName().endsWith(".ent.gz"))
                .flatMap(path -> {
                    try {
                        System.out.println(path);
                        Structure structure = StructureParser.fromInputStream(new GZIPInputStream(Files.newInputStream(path)))
                                // suppress parsing of ligand information
                                .minimalParsing(true)
                                .parse();
                        return structure.chainsWithAminoAcids()
                                .map(Chain::getChainIdentifier)
                                .map(ChainIdentifier::getFullName);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .forEach(chainId -> {
                    try {
                        System.out.println(counter[0] + " : " + chainId);
                        fileWriter.append(chainId)
                                .append(System.lineSeparator());
                        counter[0]++;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

        fileWriter.flush();
        fileWriter.close();
    }
}
