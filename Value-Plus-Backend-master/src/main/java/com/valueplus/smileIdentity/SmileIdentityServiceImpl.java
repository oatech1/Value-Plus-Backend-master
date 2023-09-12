package com.valueplus.smileIdentity;


import com.valueplus.app.exception.NotFoundException;
import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.MessageResponse;
import com.valueplus.domain.service.abstracts.HttpApiClient;
import com.valueplus.persistence.entity.KycVerification;
import com.valueplus.persistence.entity.KycVerificationActions;
import com.valueplus.persistence.entity.KycVerificationPartnerParams;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.valueplus.domain.util.UserUtils.getLoggedInUser;


@Service
@Slf4j
public class SmileIdentityServiceImpl extends HttpApiClient {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private KycVerificationPartnerParamsRepository partnerParamsRepository;
    @Autowired
    private ActionsRepository actionsRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private KycVerificationRepository verificationRepository;


    private static final List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(".png", ".jpg", ".jpeg");
    private Utilities utilitiesConnection;
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private int connectionTimeout = -1;
    private int readTimeout = -1;
    public static final String SEC_KEY = "sec_key";
    private final String URL = "https://api.smileidentity.com/v1/upload";
    @Value("${smile.identity.live.apikey}")
    private String APIKEY;
    @Value("${smile.identity.partnerId}")
    private String PARTNERID;
    @Autowired
    RestTemplate restTemplate;
    @Value("${smile.identity.live.callback}")
    private String CALLBACK_URL;
    @Value("${smile.identity.live.url}")
    private String liveUrl;



//    private String baseUrl = "https://api.smileidentity.com/v1/";

    public SmileIdentityServiceImpl(RestTemplate restTemplate) {
         super("smileIdentity", restTemplate,  "https://api.smileidentity.com/v1/");
    }

    public  Map<Object, Object> requestUploadUrl(User user) throws Exception {
        Long timeStamp = System.currentTimeMillis();
        log.info("before generate Signature class");
        System.out.println(timeStamp);
        Signature sigObj = new Signature(PARTNERID, APIKEY);
        String signature =  sigObj.getSignature(timeStamp);

        JSONObject json = new JSONObject();
        json.put("file_name",user.getFirstname().concat(user.getId().toString()).concat(".zip"));
        json.put("smile_client_id",PARTNERID);
        json.put("signature",signature);
        json.put("timestamp",new SimpleDateFormat(Signature.DATE_TIME_FORMAT).format(timeStamp));
        json.put("callback_url",CALLBACK_URL);
        json.put("model_parameters",new JSONObject());
        System.out.println(json.get("timestamp"));

        JSONObject partnerParams = new JSONObject();
        partnerParams.put("user_id",user.getId().toString());
        partnerParams.put("job_id",user.getId().toString());
        partnerParams.put("job_type","1");

        json.put("partner_params",partnerParams);


        Map<Object, Object> requestEntity = new HashMap<>();
        requestEntity.put("file_name",user.getFirstname().concat(user.getId().toString()).concat(".zip"));
        requestEntity.put("smile_client_id",PARTNERID);
        requestEntity.put("signature",signature);
        requestEntity.put("timestamp",new SimpleDateFormat(Signature.DATE_TIME_FORMAT).format(timeStamp));
        requestEntity.put("callback_url",CALLBACK_URL);
        requestEntity.put("model_parameters",new JSONObject());

        Map<Object, Object> partnerParamsEntity = new HashMap<>();
        partnerParamsEntity.put("user_id",user.getId().toString());
        partnerParamsEntity.put("job_id",passwordEncoder.encode(user.getId().toString()));
        partnerParamsEntity.put("job_type","1");

        requestEntity.put("partner_params",partnerParamsEntity);

        return requestEntity;
    }

    public SmileIdentityResponse makeUploadRequest() throws Exception {
        User user = getLoggedInUser();
        log.info("user id is " + user.getId());
        Map<Object, Object> requestUpload = requestUploadUrl(user);
        log.info("request upload is " + requestUpload);
        Map<String, String> header = new HashMap<>();
        var type = new ParameterizedTypeReference<SmileIdentityResponse>() {};
        SmileIdentityResponse result = sendRequest(HttpMethod.POST, "upload", requestUpload, header, type);
        System.out.println(result);
        JSONObject pix = new JSONObject();


        pix.put("image",toBase64());
       JSONArray pics = new JSONArray();
       pics.add(pix);
        JSONObject request = configureInfoJson(result.getUpload_url(),pics);
        ByteArrayOutputStream baos = zipUpFile(request, pics);
        uploadFile(result.getUpload_url(),baos);


        return null;
    }
    public void makeUploadRequestLive(SmileDTO smileDTO) throws Exception {
        User user = getLoggedInUser();
        Map<Object, Object> requestUpload = requestUploadUrl(user);
        Map<String, String> header = new HashMap<>();
        var type = new ParameterizedTypeReference<SmileIdentityResponse>() {};
        SmileIdentityResponse result = sendRequest(HttpMethod.POST, "upload", requestUpload, header, type);
        JSONObject pix = new JSONObject();
        pix.put("image",toBase64Live(smileDTO.getImageUrl()));
        JSONArray pics = new JSONArray();
        pics.add(pix);
        JSONObject request = configureInfoJsonLive(result.getUpload_url(),pics,smileDTO,user);
        ByteArrayOutputStream baos = zipUpFile(request, pics);
        uploadFile(result.getUpload_url(),baos);

//        KycVerification kycVerification = new KycVerification();
//        kycVerification = verificationRepository.findByUser(user);
    }

    public String toBase64() throws IOException {
        ByteArrayOutputStream bis = new ByteArrayOutputStream();
        InputStream is = null;
        URL url = new URL("https://i.ibb.co/8DRpYf7/IMG-9016.jpg");
        try {
            is = url.openStream();
            byte[] bytebuff = new byte[4096];
            int n;

            while ( (n = is.read(bytebuff)) > 0 ) {
                bis.write(bytebuff, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String encodedString = Base64.getEncoder().encodeToString(bis.toByteArray());
        System.out.println(encodedString.concat(" base 64"));
        return encodedString;

    }

    public String toBase64Live(String imageUrl) throws IOException {
        ByteArrayOutputStream bis = new ByteArrayOutputStream();
        InputStream is = null;
        URL url = new URL(imageUrl);
        try {
            is = url.openStream();
            byte[] bytebuff = new byte[4096];
            int n;

            while ( (n = is.read(bytebuff)) > 0 ) {
                bis.write(bytebuff, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String encodedString = Base64.getEncoder().encodeToString(bis.toByteArray());
        return encodedString;
    }



    private JSONObject configureInfoJson(String uploadUrl,JSONArray images) throws Exception {

        JSONObject requestEntity = new JSONObject();
        JSONObject package_information = new JSONObject();
        JSONObject id_info = new JSONObject();
        JSONObject  api_version = new JSONObject();
        api_version.put("buildNumber", 0);
        api_version.put("majorVersion", 2);
        api_version.put("minorVersion", 0);

        package_information.put("apiVersion", api_version);
//        package_information.put("language", "java");

        requestEntity.put("package_information", package_information);
        requestEntity.put("misc_information", "");

        requestEntity.put("images", configureImagePayload(images));
        requestEntity.put("server_information", uploadUrl);


//        id_info.put("dob","2000-09-20");
        id_info.put("country","NG");
        id_info.put("entered",true);
        id_info.put("id_type","NIN");
        id_info.put("id_number","47367485812");
        id_info.put("last_name","Timilehin");
        id_info.put("first_name","Awoyeye");
//        id_info.put("middle_name","Deo");

        requestEntity.put("id_info", id_info);
        return requestEntity;
    }
    private JSONObject configureInfoJsonLive(String uploadUrl,JSONArray images,SmileDTO smileDTO,User user ) throws Exception {

        JSONObject requestEntity = new JSONObject();
        JSONObject package_information = new JSONObject();
        JSONObject id_info = new JSONObject();
        JSONObject  api_version = new JSONObject();
        api_version.put("buildNumber", 0);
        api_version.put("majorVersion", 2);
        api_version.put("minorVersion", 0);

        package_information.put("apiVersion", api_version);
        requestEntity.put("package_information", package_information);
        requestEntity.put("misc_information", "");
        requestEntity.put("images", configureImagePayload(images));
        requestEntity.put("server_information", uploadUrl);

        id_info.put("country","NG");
        id_info.put("entered",true);
        id_info.put("id_type",smileDTO.getIdType());
        id_info.put("id_number",smileDTO.getIdNumber());
        id_info.put("last_name",user.getLastname());
        id_info.put("first_name",user.getFirstname());
        log.info(id_info.toString());

        requestEntity.put("id_info", id_info);
        return requestEntity;
    }
    private JSONArray configureImagePayload(JSONArray images) {
        JSONArray imagePayload = new JSONArray();
        for (Object o : images) {
            JSONObject imageObject = new JSONObject();
            if (o instanceof JSONObject) {
                Long image_type_id = (Long) ((JSONObject) o).get("image_type_id");
                imageObject.put("image_type_id", 2);
                String imageType = (String) ((JSONObject) o).get("image");
                if (SUPPORTED_IMAGE_TYPES.stream().anyMatch(imageType::endsWith)) {
                    imageObject.put("image", "");
                    String filePath = ((JSONObject) o).get("image").toString();
                    String fileName = new File(filePath).getName();
                    imageObject.put("file_name", fileName);
                } else {
                    imageObject.put("image", ((JSONObject) o).get("image"));
                    imageObject.put("file_name", "");

                }
            }
            imagePayload.add(imageObject);
        }
        return imagePayload;
    }

    private ByteArrayOutputStream zipUpFile(JSONObject infoJson, JSONArray images) throws Exception {
        // http://www.avajava.com/tutorials/lessons/how-can-i-create-a-zip-file-from-a-set-of-files.html        // https://stackoverflow.com/questions/23612864/create-a-zip-file-in-memory
         ByteArrayOutputStream baos = null;
        baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        ZipEntry entry = new ZipEntry("info.json");
        zos.putNextEntry(entry);
        zos.write(infoJson.toString().getBytes());
        log.info(infoJson.toString());
        zos.closeEntry();
        for (Object o : images) {
            if (o instanceof JSONObject) {
                String imageType = (String) ((JSONObject) o).get("image");
                if (SUPPORTED_IMAGE_TYPES.stream().anyMatch(imageType::endsWith)) {
                    String fileName = ((JSONObject) o).get("image").toString();
                    File file = new File(fileName);
                    FileInputStream fis = new FileInputStream(file);
                    ZipEntry imageEntry = new ZipEntry(file.getName());
                    zos.putNextEntry(imageEntry);
                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = fis.read(bytes)) >= 0) {
                        zos.write(bytes, 0, length);
                    }
                    zos.closeEntry();
                    baos.close();
                    fis.close();
                }
            }
        }
        zos.flush();
        zos.close();
        return baos;
    }

    private String readHttpResponse(HttpResponse response) throws Exception {
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuffer result = new StringBuffer();
        String line = "";

        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        return result.toString();
    }


    private void uploadFile(String awsUrl, ByteArrayOutputStream baos) throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPut putRequest = new HttpPut(awsUrl.trim().toString());
        putRequest.setHeader("content-type", "application/zip");
        putRequest.setEntity(new ByteArrayEntity(baos.toByteArray()));

        HttpResponse response = httpclient.execute(putRequest);
        final int statusCode = response.getStatusLine().getStatusCode();
        String strResult = readHttpResponse(response);

        if (statusCode != 200) {
            final String msg = String.format("Failed to post entity to %s, response=%d:%s - %s",
                    awsUrl, statusCode, response.getStatusLine().getReasonPhrase(), strResult);
            throw new RuntimeException(msg);
        }
    }
    public MessageResponse kycStatus(){
        MessageResponse messageResponse = new MessageResponse();
        User user = getLoggedInUser();

        KycVerification kycVerification = verificationRepository.findByUser(user).orElseThrow(() -> new NotFoundException("user not found"));

        switch (kycVerification.getResultCode()) {

            case "0810":
                messageResponse.setMessage("Kyc Verification Success");
                break;

            case "1212":
                messageResponse.setMessage("No Match When Comparing Selfie to Govt ID Authority");
                break;

            case "0911":
                messageResponse.setMessage("There was no face found in the uploaded selfie. Upload an image with a face.");
                break;

            case "1013":
                messageResponse.setMessage("The ID info was not found in the ID authority database.");
                break;

            case "0001":
                messageResponse.setMessage("Invalid Data");
                break;

            case "0903":
                messageResponse.setMessage("The uploaded Zip file is corrupted.");
                break;

            case "2314":
                messageResponse.setMessage("No Zip files was uploaded.");
                break;

            case "2215":
                messageResponse.setMessage("An existing job_id was inputted. Enter a unique job id.");
                break;

            case "0912":
                messageResponse.setMessage("The uploaded selfie quality was too poor. Upload a higher quality selfie.");
                break;

            case "0814":
                messageResponse.setMessage("The machine thinks there is a possible spoof but can't make a final decision. A document reviewer will check the Selfie for potential fraud attempts.");
                break;


            case "1213":
                messageResponse.setMessage("The document reviewer is not sure if there is a possible fraud attempt on the Selfie.");
                break;

            case "0815":
                messageResponse.setMessage("The machine is not sure if both images match, so a document reviewer will compare.");
                break;

        }
        return messageResponse;
    }


    public void callBack(SmileCallback smileCallback){
             KycVerification kycVerification = new KycVerification();
            KycVerificationPartnerParams partnerParams = new KycVerificationPartnerParams();
            KycVerificationActions kycVerificationActions = new KycVerificationActions();
                //KycVerificationPartnerParams partnerParams1 = partnerParamsRepository.findByJobId(smileCallback.getPartnerParams().getJob_id()).orElse(null);
                KycVerificationActions verificationActions = actionsRepository.findByJobId(smileCallback.getSmileJobID());
                if (verificationActions != null){
                partnerParams = partnerParamsRepository.findByJobId(smileCallback.getPartnerParams().getJob_id()).orElse(null);
                kycVerification = verificationRepository.findBySmileJobID(smileCallback.getSmileJobID());
                kycVerificationActions = actionsRepository.findByJobId(smileCallback.getSmileJobID());
                    System.out.println("is not null");
                }

                User user = userRepository.findById(Long.valueOf(smileCallback.getPartnerParams().getUser_id())).orElse(null);
                if (smileCallback.getResultText().equalsIgnoreCase("Enroll User")){
                    System.out.println(true);
                user.setKycVerification(true);}
                else {user.setKycVerification(false);}
                userRepository.save(user);

                partnerParams.setJobId(smileCallback.getPartnerParams().getJob_id());

                partnerParams.setJobType(smileCallback.getPartnerParams().getJob_type());

                partnerParams.setUserId(smileCallback.getPartnerParams().getUser_id());

                kycVerificationActions.setLivenessCheck(smileCallback.getActions().getLivenessCheck());

                kycVerificationActions.setRegisterSelfie(smileCallback.getActions().getRegisterSelfie());

                kycVerificationActions.setSelfieProvided(smileCallback.getActions().getSelfieProvided());

                kycVerificationActions.setVerifyIdNumber(smileCallback.getActions().getVerifyIdNumber());

                kycVerificationActions.setReturnPersonalInfo(smileCallback.getActions().getReturnPersonalInfo());

                kycVerificationActions.setSelfieToIdAuthorityCompare(smileCallback.getActions().getSelfieToIdAuthorityCompare());

                kycVerificationActions.setUser(user);
                kycVerificationActions.setJobId(smileCallback.getSmileJobID());

                partnerParamsRepository.save(partnerParams);
                actionsRepository.save(kycVerificationActions);

                KycVerificationActions verificationActionss = actionsRepository.findByUser(user);

                KycVerificationPartnerParams verificationPartnerParams = partnerParamsRepository.findByUserId(user.getId().toString());

                kycVerification.setConfidenceValue(smileCallback.getConfidenceValue());
                kycVerification.setSmileJobID(smileCallback.getSmileJobID());
                kycVerification.setResultText(smileCallback.getResultText());
                kycVerification.setResultCode(smileCallback.getResultCode());
                kycVerification.setActions(verificationActionss);
                kycVerification.setPartnerParams(verificationPartnerParams);
                kycVerification.setUser(user);

                verificationRepository.save(kycVerification);

        System.out.println(kycVerification);

    }


}
