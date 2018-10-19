package de.bioforscher.efr;

import de.bioforscher.efr.model.Protein;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping(value = "/api/", method = RequestMethod.GET)
public class EarlyFoldingController {
    private static final String EXAMPLE_DATA_ID = "1acj_A";
    private Protein exampleProtein;
    private List<String> chainIds;

    @PostConstruct
    public void activate() {
        //TODO replace with actual data
        this.chainIds = Stream.of("1acj_A", "1c0a_A", "1k1i_A", "4ins_A", "2qho_A")
                .collect(Collectors.toList());
        this.exampleProtein = null;
    }

    @RequestMapping(value= "/id/{id}", method = RequestMethod.GET)
    public Protein getProtein(@PathVariable String id) {
        if(EXAMPLE_DATA_ID.equals(id)) {
            return exampleProtein;
        }

        String[] split = id.split("_");
        String pdbId = split[0];
        String chainid = split[1];
        //TODO impl

        return null;
    }

    @RequestMapping(value = "/submit", method = RequestMethod.POST, consumes = "text/plain")
    public Protein submit(@RequestBody String postPayload) {
        //TODO impl
        return null;
    }

    @RequestMapping(value = "/complete/{query}", method = RequestMethod.GET)
    public List<String> complete(@PathVariable String query) {
        return chainIds.stream()
                .filter(chainId -> chainId.startsWith(query))
                .collect(Collectors.toList());
    }
}
