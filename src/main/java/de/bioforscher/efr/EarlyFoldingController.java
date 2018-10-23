package de.bioforscher.efr;

import de.bioforscher.efr.model.Protein;
import de.bioforscher.jstructure.model.feature.ComputationException;
import de.bioforscher.jstructure.model.structure.Chain;
import de.bioforscher.jstructure.model.structure.Structure;
import de.bioforscher.jstructure.model.structure.StructureParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/", method = RequestMethod.GET)
public class EarlyFoldingController {
    private static final Logger logger = LoggerFactory.getLogger(EarlyFoldingController.class);
    private static final String EXAMPLE_DATA_ID = "1acj_A";
    /**
     *  the flag indicating that a file was uploaded (with need to be base64-encoded to be processed by the REST-
     *  interface) - this value can also be used to decode the submitted string as e.g. the header information has be
     *  removed
     */
    private static final String FILE_ENCODING_FLAG = "base64,";
    private Path pdbDirectory;
    private Protein exampleProtein;
    /**
     * collection of PDB chain ids: maps both chars in the middle to all chain ids in this bin
     * e.g.: ac -> [1acj_A, 3ace_A, 3ace_B]
     */
    private Map<String, List<String>> chainIds;
    private EarlyFoldingClassifier earlyFoldingClassifier;

    @PostConstruct
    public void activate() {
        // specify pdb directory
        this.pdbDirectory = Paths.get("/var/local/pdb/");
        StructureParser.OptionalSteps.setLocalPdbDirectory(pdbDirectory);

        //TODO rsync PDB distribution on server

        // initialize all chain ids from preprocessed file (mere lines of ids in format: 1acj_A)
        logger.info("initializing chain id list for auto-completion of user input");
        try {
            try(InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("data/chainids.dat")) {
                try(InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
                    try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                        chainIds = bufferedReader.lines()
                                .collect(Collectors.groupingBy(chainId -> chainId.substring(1, 3)));
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        logger.info("registered {} valid amino acid chains",
                chainIds.values()
                        .stream()
                        .mapToInt(Collection::size)
                        .sum());

        this.earlyFoldingClassifier = EarlyFoldingClassifier.getInstance();

        // create example data container
        logger.info("creating example data {}", EXAMPLE_DATA_ID);
        String[] exampleSplit = EXAMPLE_DATA_ID.split("_");
        Chain chain = StructureParser.fromPdbId(exampleSplit[0]).parse().select().chainId(exampleSplit[1]).asChain();
        List<String> csvLines;
        try {
            try(InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("data/example.csv")) {
                try(InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
                    try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                        csvLines = bufferedReader.lines().collect(Collectors.toList());
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.exampleProtein = new Protein(chain,
                csvLines.stream()
                        .collect(Collectors.joining(System.lineSeparator())),
                csvLines.stream()
                        .filter(line -> line.endsWith("early"))
                        .map(line -> line.split(","))
                        .map(split -> split[2] + "-" + split[1])
                        .collect(Collectors.toList()));
    }

    @RequestMapping(value= "/id/{id}", method = RequestMethod.GET)
    public Protein getProtein(@PathVariable String id) {
        logger.info("providing protein with id {}", id);
        if(EXAMPLE_DATA_ID.equals(id)) {
            return exampleProtein;
        }

        String[] split = id.split("_");
        String pdbId = split[0];
        String chainId = split[1];

        Chain chain = StructureParser.fromPdbId(pdbId).parse()
                .select()
                .chainId(chainId)
                .asChain();
        return processChain(chain);
    }

    private Protein processChain(Chain chain) {
        EarlyFoldingClassifier.EarlyFoldingClassification earlyFoldingClassification = earlyFoldingClassifier.process(chain);

        Protein protein = new Protein(chain,
                earlyFoldingClassification.getCsvRepresentation(),
                earlyFoldingClassification.getEarlyFoldingResidues());
        logger.info("created protein {}_{} with EFR: {}",
                protein.getPdbId(),
                protein.getChainId(),
                protein.getEarlyFoldingResidues());
        return protein;
    }

    @RequestMapping(value = "/submit", method = RequestMethod.POST, consumes = "text/plain")
    public Protein submit(@RequestBody String postPayload) {
        String structureData = postPayload.substring(postPayload.indexOf(FILE_ENCODING_FLAG) + FILE_ENCODING_FLAG.length(), postPayload.length() - 2);
        logger.info("processing submitted structure");

        try {
            byte[] uploadedFileContent = Base64.getDecoder().decode(structureData);
            Structure structure = StructureParser.fromInputStream(new ByteArrayInputStream(uploadedFileContent)).parse();
            Chain chain = structure.select().asChain();
            return processChain(chain);
        } catch (NullPointerException e) {
            // NPE happens when ProteinIdentifier cannot be set
            throw new ComputationException("no valid file content");
        } catch (Exception e) {
            // wrap generic exceptions
            throw new ComputationException(e);
        }
    }

    @RequestMapping(value = "/complete/{query}", method = RequestMethod.GET)
    public List<String> complete(@PathVariable String query) {
        String middlePart = query.substring(1, 3);

        return chainIds.get(middlePart)
                .stream()
                .filter(chainId -> chainId.startsWith(query))
                .collect(Collectors.toList());
    }
}
