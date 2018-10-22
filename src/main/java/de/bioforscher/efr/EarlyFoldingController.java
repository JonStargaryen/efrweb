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
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private Protein exampleProtein;
    private List<String> chainIds;
    private EarlyFoldingClassifier earlyFoldingClassifier;

    @PostConstruct
    public void activate() {
        //TODO replace with actual data
        this.chainIds = Stream.of("1acj_A", "1c0a_A", "1k1i_A", "4ins_A", "2qho_A")
                .collect(Collectors.toList());
        this.earlyFoldingClassifier = EarlyFoldingClassifier.getInstance();

        // create example data container
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
        List<String> matchingChainIds = chainIds.stream()
                .filter(chainId -> chainId.startsWith(query))
                .collect(Collectors.toList());
        logger.info("completed search text {} to {}",
                query,
                matchingChainIds);
        return matchingChainIds;
    }
}
