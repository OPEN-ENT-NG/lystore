package fr.openent.lystore.service.impl;

import fr.openent.lystore.model.Order;
import fr.openent.lystore.model.Structure;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.UserInfos;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.http.Renders.badRequest;

public class EmailSendService {

    private Neo4j neo4j;

    private static final io.vertx.core.logging.Logger log = LoggerFactory.getLogger (EmailSendService.class);
    private final EmailSender emailSenderDefault;
    public EmailSendService(EmailSender emailSender){
        this.emailSenderDefault = emailSender;
        this.neo4j = Neo4j.getInstance();

    }

    public void sendMail(HttpServerRequest request, String eMail, String object, String body, EmailSender emailSender) {
        sendMail(request,eMail,null,object,body,emailSender);
    }
    public void sendMail(HttpServerRequest request, String eMail, String cc, String object, String body , EmailSender emailSender) {
        if(emailSender != null){
            emailSender.sendEmail(request,
                    eMail,
                    cc,
                    null,
                    object,
                    body,
                    null,
                    true,
                    null);
        }else {
            emailSenderDefault.sendEmail(request,
                    eMail,
                    cc,
                    null,
                    object,
                    body,
                    null,
                    true,
                    null);
        }
    }

    public void sendMails(HttpServerRequest request, JsonObject result, JsonArray rows, UserInfos user, String url,
                          JsonArray structureRows){
        final int contractNameIndex = 1;
        final int agentEmailIndex = 3;
        JsonObject structRow;
        JsonArray row;
        String oldIdStruct = "",currentIdStruct, nameEtab = "";
        String number_validation = result.getString("number_validation");
        ArrayList<Integer> idsCampaign =  new ArrayList<>();
        JsonArray line = rows.getJsonArray(0);
        String agentMailObject = "[LyStore] Commandes " + line.getString(contractNameIndex);
        String agentMailBody = getAgentBodyMail(line, user, number_validation, url);
        JsonArray mailsRow = new JsonArray();
        sendMail(request, line.getString(agentEmailIndex),
                agentMailObject,
                agentMailBody,null);

        for(int i = 0 ; i < rows.size(); i++){
            row = rows.getJsonArray(i);
            currentIdStruct = row.getString(4);
            Integer idCampaign = row.getInteger(5);

            if(!oldIdStruct.equals(currentIdStruct)){
                oldIdStruct = currentIdStruct;
                if(i != 0){
                    mailsToClient(request, result, user, url, nameEtab, idsCampaign, mailsRow);
                    idsCampaign =  new ArrayList<>();
                }
                for(int j =0; j < structureRows.size(); j++){
                    structRow = structureRows.getJsonObject(j);
                    if(structRow.getString("id").equals(currentIdStruct)){
                        nameEtab = structRow.getString("name");
                        mailsRow = structRow.getJsonArray("mails");
                    }
                }
            }
            if(!idsCampaign.contains(idCampaign)){
                idsCampaign.add(idCampaign);
            }
        }

        mailsToClient(request, result, user, url, nameEtab, idsCampaign, mailsRow);


    }

    private void mailsToClient(HttpServerRequest request,JsonObject result, UserInfos user, String url, String nameEtab, ArrayList<Integer> idsCampaign, JsonArray mailsRow) {
        for (int k = 0; k < mailsRow.size(); k++) {
            JsonObject userMail = mailsRow.getJsonObject(k);
            String mailObject = "[LyStore] Commandes ";
            if (userMail.getString("mail") != null) {
                String mailBody = getStructureBodyMail(mailsRow.getJsonObject(k), user,
                        result.getString("number_validation"), url, nameEtab,idsCampaign);
                sendMail(request, userMail.getString("mail"),
                        mailObject,
                        mailBody,null);
            }
        }
    }


    public void getPersonnelMailStructure(JsonArray structureIds, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (w:WorkflowAction {displayName: 'lystore.access'})--(r:Role) with r , count((r)-->(w)) as NbrRows " +
                " Match p = ((r)<--(mg:ManualGroup)-->(s:Structure)), (mg)<-[IN]-(u:User)  " +
                "where NbrRows=1 AND s.id IN {ids} return s.id as id, s.name as name, " +
                "collect(DISTINCT {mail : u.email, name: u.displayName} ) as mails ";
        neo4j.execute(query, new JsonObject().put("ids", structureIds),
                Neo4jResult.validResultHandler(handler));

    }

    private static String getStructureBodyMail(JsonObject row, UserInfos user, String numberOrder, String url,
                                               String name, ArrayList<Integer> idsCampaign){
        StringBuilder listOrders= new StringBuilder();
        for(int i = 0;i < idsCampaign.size(); i++){
            listOrders.append("<br />").append(url).append("#/campaign/").append(idsCampaign.get(i)).append("/order <br />");
        }
        String body = "Bonjour " + row.getString("name") + ", <br/> <br/>"
                + "Une commande sous le numéro \"" + numberOrder + "\" vient d'être validée."
                + " Une partie de la commande concerne l'établissement " + name + ". "
                + "Cette confirmation est visible sur l'interface de LyStore en vous rendant ici :  <br />"
                + listOrders
                + "<br /> Bien Cordialement, "
                + "<br /> L'équipe LyStore. ";
        return formatAccentedString(body);

    }
    private static String getAgentBodyMail(JsonArray row, UserInfos user, String numberOrder, String url){
        final int contractName = 2 ;
        String body = null;
        body = "Bonjour " + row.getString(contractName) + ", <br/> <br/>"
                + user.getFirstName() + " " + user.getLastName() + " vient de valider une commande sous le numéro \""
                + numberOrder + "\"."
                + " Une partie de la commande concerne le marché " + row.getString(1) + ". "
                + "<br /> Pour générer le bon de commande et les CSF associés, il suffit de se rendre ici : <br />"
                + "<br />" + url  + "#/order/valid" + "<br />"
                + "<br /> Bien Cordialement, "
                + "<br /> L'équipe LyStore. ";
        return formatAccentedString(body);
    }

    public void sendMailsNotificationsEtab(HttpServerRequest request, Map<Structure, List<Order>> structureOrderMap, String domainMail, EmailSender emailSend){
        try{ for (Map.Entry<Structure, List<Order>> structureOrderEntry : structureOrderMap.entrySet()) {
            String userMail = structureOrderEntry.getKey().getUAI();
            Order order = structureOrderEntry.getValue().get(0);
            String mailObject = getNotificationObjectMail(order.getBcOrder().getNumber(),order.getMarket().getMarket_number(),
                    order.getMarket().getName(),structureOrderEntry.getKey().getUAI());
            if (userMail != null) {
                String mailBody = getNotificationBodyMail(structureOrderEntry.getValue(),structureOrderEntry.getKey());
                sendMail(request, structureOrderEntry.getKey().getUAI() + "@" + domainMail ,
                        mailObject,
                        mailBody,
                        emailSend);
            }
        }
            request.response().setStatusCode(200).end();
        } catch (Exception e) {
            badRequest(request,e.getMessage());

        }
    }

    private String getNotificationBodyMail(List<Order> orders,Structure structure) {

        StringBuilder body = new StringBuilder();
        body = new StringBuilder("Madame, Monsieur <br /> <br />"
                + "Les équipements ci-dessous demandés sur le système d'information LYSTORE viennent d'être commandés pour votre établissement: " + structure.getName()
                + "<br/> Liste des matériels à venir: "
                + "<br/>"
                + "<table>  ");
        for(Order order : orders){
            body.append(" <tr>" + "<td>- ")
                    .append(order.getName())
                    .append(" </td>")
                    .append("<td style=\"padding-left:5px;\">  Quantité: ")
                    .append(order.getAmount())
                    .append("</td> ")
                    .append("</tr>")
            ;
        }

        body.append("</table><br/> Cordialement, " + "<br/>" + "<br/> <b> Service de la Transformation Numérique des Lycées </b>" +
                "<br/>Direction Numérique, Innovation et Smart Région | Pôle Transformation Numérique");
        return formatAccentedString(body.toString());
    }

    private static String getNotificationObjectMail(String bcNumber, String marketNumber, String marketName, String codeUai){
        String object = null;
        object = "[LYSTORE - BC N°: " + bcNumber
                + " - MARCHE N°: "
                + marketNumber
                + " - "
                + marketName
                + " - Commande dotation équipements informatiques] "
                + codeUai;
        return object;
    }
    public void sendMailsHelpDesk(HttpServerRequest request, Map<Structure, List<Order>> structureOrderMap, String domainMail, EmailSender emailSend, String recipientMail) {

        try {
            for (Map.Entry<Structure, List<Order>> structureOrderEntry : structureOrderMap.entrySet()) {
                String userMail = structureOrderEntry.getKey().getUAI();
                Order order = structureOrderEntry.getValue().get(0);
                String mailObject = getNotificationObjectMail(order.getBcOrder().getNumber(), order.getMarket().getMarket_number(),
                        order.getMarket().getName(), structureOrderEntry.getKey().getUAI());
                if (userMail != null) {
                    String mailBody = getNotificationDeskBodyMail(structureOrderEntry.getValue(), structureOrderEntry.getKey());
                    sendMail(request, recipientMail,
                            structureOrderEntry.getKey().getUAI() + "@" + domainMail,
                            mailObject,
                            mailBody,
                            emailSend
                    );
                }
            }
            request.response().setStatusCode(200).end();
        } catch (Exception e) {
            badRequest(request,e.getMessage());

        }
    }

    private String getFormatDate(String date) {
        date = date.replace("T"," ");
        SimpleDateFormat formatterDateSQL = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat formatterDate = new SimpleDateFormat("dd/MM/yyyy");
        Date orderDate = null;
        try {
            orderDate = formatterDateSQL.parse(date);
            return formatterDate.format(orderDate);
        } catch (ParseException e) {
            log.error("Incorrect date format : " + date);
            return "";
        }
    }
    private String getNotificationDeskBodyMail(List<Order> orders, Structure structure) {
        String  body = "Madame, Monsieur, <br/>  <br/> " +
                "Ce ticket a été créé par le Service de la Transformation Numérique des Lycées dans le cadre du suivi de livraison des matériels informatiques fournis par la Région Île-de-France, et pour laquelle votre établissement fait l'objet d'une dotation suite à la commande passée sur le système d'information LYSTORE. <br/> " +
                " <br/> " +

                "<b>Cadre : Dotation EPLE - Commande de matériel sur le système d'information LYSTORE</b> " +
                " <br/>" +
                "<br/> " +
                "Délai de livraison estimé à 42 jours maximum, à partir du : ";
        body += getFormatDate(orders.get(0).getBcOrder().getDateCreation());
        body += "" +
                " <br/> " +
                " <br/> - Si vous souhaitez définir une préférence dans l'organisation de la livraison du matériel attendus," +
                " vous êtes invité à alimenter le ticket correspondant dans le système d'information CESAME." +
                " <br/> " +
                "- Important : par défaut, la livraison prévoit une prestation de mise en service des matériels." +
                " Dans le cas où vous ne souhaitez pas bénéficier de cette prestation à la livraison," +
                " veuillez l’indiquer aussi dans le ticket correspondant dans le système d'information CESAME. " +
                "<br/> " +
                "<br/> " +
                "Toute information saisie dans ce ticket sera transmise à notre prestataire." +
                "<br/> " +
                "Celui-ci vous contactera afin de confirmer la date de livraison et la prestation." +
                "<br/> " +
                "<br/> " +
                "Notez toute fois que cette date, et la prestation de mise en service, ne pourront pas être modifiées " +
                "pour une question logistique et organisationnelle qui impacte les établissements de toute l'Île-de-France." +
                " <br/> " +
                " <br/> <b> Modalité de réception et signature</b><br/> " +
                "- Nous rappelons que la présence d'un membre de l'équipe de direction (Chef d'établissement, Gestionnaire,...) est impérative à la livraison. Eux seuls sont habilités à signer le bon de livraison et/ou le certificat. <br/> " +
                "- L'établissement doit préparer le lieux de stockage des matériels à l'avance. La livraison ne pourra se faire à la loge.<br/> " +
                "<br/><b>Modalité de Support et délai de contrôle</b><br/> " +
                "- La panne au déballage doit être constatée dans les 5 jours ouvrés suivants la livraison et doit être déclarée auprès de la Région Île-de-France dans un délai de 7 jours ouvrés à compter de la réception. Au-delà, toute anomalie sera prise en charge selon la procédure standard de support et non comme une panne au déballage. <br/> " +
                "- Toute panne ultérieure devra faire l'objet d'un ticket dédié dans le système d'information CESAME où sera stipulé le numéro d'étiquette RIDF du matériel en cause. <br/> " +
                " <br/> " +
                "<b>Liste des matériels à venir :</b> <br/>" +
                "<br/> " +
                "<table>";
        for(Order order : orders){
            body +=    " <tr>"
                    + "<td>- "+order.getName() +" </td>"
                    + "<td style=\"padding-left:5px;\">  Quantité: " + order.getAmount() + "</td> "
                    +"</tr>"
            ;
        }
        body +=" </table> <br/> Service de la Transformation Numérique des Lycées<br/> " +
                "Direction Numérique, Innovation et Smart Région | Pôle Transformation Numérique<br/> " +
                " <br/> " +
                "N.B. : Le ticket sera fermé et" +
                " déclaré « conforme » une fois les livrables (INV,BL,CSF) déposés dans le présent ticket. Le ticket passera alors en « résolu ».<br/> ";

        return formatAccentedString(body);
    }


    private static String getEncodedRedirectUri(String callback) {
        try {
            return "/auth/login?callback=" + URLEncoder.encode(callback, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private static String formatAccentedString (String body){
        return  body.replace("&","&amp;").replace("€","&euro;")
                .replace("à","&agrave;").replace("â","&acirc;")
                .replace("é","&eacute;").replace("è","&egrave;")
                .replace("ê","&ecirc;").replace("î","&icirc;")
                .replace("ï","&iuml;") .replace("œ","&oelig;")
                .replace("ù","&ugrave;").replace("û","&ucirc;")
                .replace("ç","&ccedil;").replace("À","&Agrave;")
                .replace("Â","&Acirc;").replace("É","&Eacute;")
                .replace("È","&Egrave;").replace("Ê","&Ecirc;")
                .replace("Î","&Icirc;").replace("Ï","&Iuml;")
                .replace("Œ","&OElig;").replace("Ù","&Ugrave;")
                .replace("Û","&Ucirc;").replace("Ç","&Ccedil;");

    }


}
