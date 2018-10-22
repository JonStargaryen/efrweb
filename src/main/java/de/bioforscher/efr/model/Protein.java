package de.bioforscher.efr.model;

import de.bioforscher.jstructure.model.structure.Chain;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Protein {
    private final String csvRepresentation;
    private final String pdbRepresentation;
    private final List<String> earlyFoldingResidues;
    private final String pdbId;
    private final String chainId;
    private final String title;

    public Protein(Chain chain,
                   String csvRepresentation,
                   List<String> earlyFoldingResidues) {
        this.csvRepresentation = csvRepresentation;
        this.pdbRepresentation = chain.getPdbRepresentation();
        this.pdbId = chain.getChainIdentifier().getProteinIdentifier().getPdbId();
        this.chainId = chain.getChainIdentifier().getChainId();
        this.title = chain.getParentStructure().getTitle();

        // sort efr again
        this.earlyFoldingResidues = earlyFoldingResidues.stream()
                //TODO may die on insertion codes
                .sorted(Comparator.comparingInt(string -> Integer.valueOf(string.split("-")[1])))
                .collect(Collectors.toList());
    }

    public String getCsvRepresentation() {
        return csvRepresentation;
    }

    public String getPdbRepresentation() {
        return pdbRepresentation;
    }

    public List<String> getEarlyFoldingResidues() {
        return earlyFoldingResidues;
    }

    public String getPdbId() {
        return pdbId;
    }

    public String getChainId() {
        return chainId;
    }

    public String getTitle() {
        return title;
    }
}
