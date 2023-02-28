package wsaa;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.Base64;
import org.apache.axis.encoding.XMLType;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.dom4j.io.SAXReader;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.rpc.ParameterMode;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.util.*;
import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.logging.Logger;
import org.apache.axis.AxisFault;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class WSAAClient {
    
    private static final String TA_PATH = "wsafip/AccessTickets/TA-{SERVICE}.xml";
    private static final Logger LOG = Logger.getLogger(WSAAClient.class.getName());
    
    private File sourceTA(String service) throws Exception {
        
        ClassLoader classLoader = getClass().getClassLoader();
        
        String serviceString = TA_PATH.replace("{SERVICE}", service);
        
        File file = new File(classLoader.getResource(serviceString).getFile());
        
        if (file.createNewFile()) {
            System.out.println("File is created!");
        } else {
            System.out.println("File already exists.");
        }
        
        return file;
    }
    
    private void saveTA(String xmlSource, String service) throws Exception {
        PrintWriter pw = new PrintWriter(sourceTA(service), "UTF-8");
        System.out.println(xmlSource);
        pw.write(xmlSource);
        pw.close();
    }
    
    private Map<String, String> readTA(String service) throws Exception {
        Map<String, String> resultado = new HashMap<String, String>();
        
        try {
            org.dom4j.Document tokenDoc = new SAXReader(false).read(new FileInputStream(sourceTA(service)));
            
            resultado.put("token", tokenDoc.valueOf("/loginTicketResponse/credentials/token"));
            resultado.put("sign", tokenDoc.valueOf("/loginTicketResponse/credentials/sign"));
            resultado.put("expirationTime", tokenDoc.valueOf("/loginTicketResponse/header/expirationTime"));
            
        } catch (Exception exc) {
            LOG.severe(exc.toString());
        }
        
        return resultado;
    }
    
    public Map<String, String> searchTA(String service) throws Exception {
        
        String token = null;
        String sign = null;
        
        Map<String, String> afipMap = readTA(service);
        if (afipMap.containsKey("expirationTime")) {
            entities.Date fechaArchivo = new entities.Date(afipMap.get("expirationTime"), entities.Date.FECHA_HORA_AFIP);
            
            if (fechaArchivo.compareTo(new entities.Date()) > 0) {
                token = afipMap.get("token");
                sign = afipMap.get("sign");
            }
        }
        
        if (token == null) {
            
            try {
                
                String loginTicketResponse = this.requestToken(service);
                
                Reader tokenReader = new StringReader(loginTicketResponse);
                org.dom4j.Document tokenDoc = new SAXReader(false).read(tokenReader);
                
                saveTA(loginTicketResponse, service);
                
                token = tokenDoc.valueOf("/loginTicketResponse/credentials/token");
                sign = tokenDoc.valueOf("/loginTicketResponse/credentials/sign");
            } catch (AxisFault e) {
                LOG.severe(e.getFaultString() + "(" + e.getFaultCode().toString() + ")");
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error de auth");
            }
        }
        
        Map<String, String> resultant = new HashMap<String, String>();
        resultant.put("token", token);
        resultant.put("sign", sign);
        
        return resultant;
    }
    
    private String requestToken(String service) throws Exception {
        Properties config = new Properties();
        
        try {
            config.load(Files.newInputStream(Paths.get("src/main/resources/wsaa_client.properties")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        String endpoint = config.getProperty("endpoint");
        String dstDN = config.getProperty("dstdn");
        
        String p12file = config.getProperty("keystore");
        String signer = config.getProperty("keystore-signer");
        String p12pass = config.getProperty("keystore-password");
        
        long TicketTime = Long.parseLong(config.getProperty("TicketTime"));
        
        byte[] LoginTicketRequestXmlCms = createCMS(p12file, p12pass, signer, dstDN, service, TicketTime);
        
        return invoke(LoginTicketRequestXmlCms, endpoint);
    }
    
    private String invoke(byte[] LoginTicketRequest_xml_cms, String endpoint) throws Exception {
        
        String LoginTicketResponse = null;
        try {
            
            Service service = new Service();
            Call call = (Call) service.createCall();

            //
            // Prepare the call for the Web service
            //
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("loginCms");
            call.addParameter("request", XMLType.XSD_STRING, ParameterMode.IN);
            call.setReturnType(XMLType.XSD_STRING);

            //
            // Make the actual call and assign the answer to a String
            //
            LoginTicketResponse = (String) call.invoke(new Object[]{
                Base64.encode(LoginTicketRequest_xml_cms)});
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (LoginTicketResponse);
    }

    //
    // Create the CMS Message
    //
    private byte[] createCMS(String p12file, String p12pass, String signer, String dstDN, String service, Long TicketTime) {
        
        PrivateKey pKey = null;
        X509Certificate pCertificate = null;
        byte[] asn1_cms = null;
        CertStore cstore = null;
        String LoginTicketRequest_xml;
        String SignerDN = null;

        //
        // Manage Keys & Certificates
        //
        try {
            // Create a keystore using keys from the pkcs#12 p12file
            KeyStore ks = KeyStore.getInstance("pkcs12");
            FileInputStream p12stream = new FileInputStream(p12file);
            ks.load(p12stream, p12pass.toCharArray());
            p12stream.close();

            // Get Certificate & Private key from KeyStore
            pKey = (PrivateKey) ks.getKey(signer, p12pass.toCharArray());
            pCertificate = (X509Certificate) ks.getCertificate(signer);
            SignerDN = pCertificate.getSubjectDN().toString();

            // Create a list of Certificates to include in the final CMS
            ArrayList<X509Certificate> certList = new ArrayList<X509Certificate>();
            certList.add(pCertificate);
            
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            
            cstore = CertStore.getInstance("Collection", new CollectionCertStoreParameters(certList), "BC");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //
        // Create CMS Message
        //
        try {
            //
            // Create XML Message
            //
            LoginTicketRequest_xml = createLoginTicketRequest(SignerDN, dstDN, service, TicketTime);

            // Create a new empty CMS Message
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

            // Add a Signer to the Message
            gen.addSigner(pKey, pCertificate, CMSSignedDataGenerator.DIGEST_SHA1);

            // Add the Certificate to the Message
            gen.addCertificatesAndCRLs(cstore);

            // Add the data (XML) to the Message
            CMSProcessable data = new CMSProcessableByteArray(LoginTicketRequest_xml.getBytes());

            // Add a Sign of the Data to the Message
            CMSSignedData signed = gen.generate(data, true, "BC");

            //
            asn1_cms = signed.getEncoded();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return (asn1_cms);
    }

    //
    // Create XML Message for AFIP wsaa
    //
    private String createLoginTicketRequest(String SignerDN, String dstDN, String service, Long TicketTime) throws DatatypeConfigurationException {
        
        String LoginTicketRequest_xml;
        
        Date GenTime = new Date();
        GregorianCalendar gentime = new GregorianCalendar();
        GregorianCalendar exptime = new GregorianCalendar();
        String UniqueId = String.valueOf(GenTime.getTime() / 1000);
        
        exptime.setTime(new Date(GenTime.getTime() + TicketTime));
        
        XMLGregorianCalendar XMLGenTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(gentime);
        XMLGregorianCalendar XMLExpTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(exptime);
        
        LoginTicketRequest_xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<loginTicketRequest version=\"1.0\">"
                + "<header>"
                + "<source>" + SignerDN + "</source>"
                + "<destination>" + dstDN + "</destination>"
                + "<uniqueId>" + UniqueId + "</uniqueId>"
                + "<generationTime>" + XMLGenTime + "</generationTime>"
                + "<expirationTime>" + XMLExpTime + "</expirationTime>"
                + "</header>"
                + "<service>" + service + "</service>"
                + "</loginTicketRequest>";
        
        return (LoginTicketRequest_xml);
    }
    
}
