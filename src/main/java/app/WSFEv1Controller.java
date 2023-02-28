package app;

import entities.StatusMessageResponse;
import homo.wsfev1.DocTipoResponse;
import homo.wsfev1.FEAuthRequest;
import homo.wsfev1.DocTipo;
import homo.wsfev1.Service;
import homo.wsfev1.ServiceSoap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import org.springframework.http.HttpStatus;
import wsaa.WSAAClient;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("wsfe")
public class WSFEv1Controller {
    
    private static final String SERVICE_REFERENCE = "wsfe";
    private static final Logger LOG = Logger.getLogger(WSFEv1Controller.class.getName());
    
    @GetMapping("")
    public StatusMessageResponse status() {
        return new StatusMessageResponse(200, WSFEv1Controller.class.getSimpleName() + " found");
    }
    
    @GetMapping("/testaa")
    public List<DocTipo> testAA() {
        try {
            
            WSAAClient wsaaClient = new WSAAClient();
            
            ResourceBundle bundle = ResourceBundle.getBundle("wsaa_client");
            
            Map<String, String> afipMap = wsaaClient.searchTA(SERVICE_REFERENCE);
            
            Service service = new Service();
            ServiceSoap soap = (ServiceSoap) service.getServiceSoap();
            
            FEAuthRequest auth = new FEAuthRequest();
            auth.setCuit(Long.parseLong(bundle.getString("CUIT")));
            auth.setToken(bundle.getString(afipMap.get("token")));
            auth.setSign(bundle.getString(afipMap.get("sign")));
            
            DocTipoResponse response = soap.feParamGetTiposDoc(auth);
            
            return response.getResultGet().getDocTipo();
            
        } catch (Exception e) {
            LOG.severe(e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No results");
        }
    }
}
