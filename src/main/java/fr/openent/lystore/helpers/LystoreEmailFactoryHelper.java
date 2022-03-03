package fr.openent.lystore.helpers;

import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.email.GoMailSender;
import fr.wseduc.webutils.email.SMTPSender;
import fr.wseduc.webutils.email.SendInBlueSender;
import fr.wseduc.webutils.exception.InvalidConfigurationException;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.email.SmtpSender;
import org.entcore.common.email.impl.PostgresEmailSender;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.email.GoMailSender;
import fr.wseduc.webutils.email.SMTPSender;
import fr.wseduc.webutils.email.SendInBlueSender;
import fr.wseduc.webutils.exception.InvalidConfigurationException;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import java.net.URISyntaxException;
import org.entcore.common.email.impl.PostgresEmailSender;
import java.net.URISyntaxException;

public class LystoreEmailFactoryHelper {
    //
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//
    public static final int PRIORITY_VERY_LOW = -2;
    public static final int PRIORITY_LOW = -1;
    public static final int PRIORITY_NORMAL = 0;
    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_VERY_HIGH = 2;
    private final Vertx vertx;
    private final JsonObject config;
    private final JsonObject moduleConfig;
    private final Logger log;

    public LystoreEmailFactoryHelper(Vertx vertx) {
        this(vertx, (JsonObject)null);
    }

    public LystoreEmailFactoryHelper(Vertx vertx, JsonObject config) {
        this(vertx, config,null);
    }

    public LystoreEmailFactoryHelper(Vertx vertx, JsonObject config,String emailSender) {
        this.log = LoggerFactory.getLogger(org.entcore.common.email.EmailFactory.class);
        this.vertx = vertx;
        if (config != null && config.getJsonObject("emailConfig") != null) {
            this.config = config.getJsonObject("emailConfig");
            this.moduleConfig = config;
        } else {
            LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
            String s = (String)server.get("emailConfig");
            if (s != null) {
                this.config = new JsonObject(s);
                if(emailSender != null)
                    this.config.put("email",emailSender);
                this.moduleConfig = this.config;
            } else {
                this.config = null;
                this.moduleConfig = null;
            }
        }

    }


    public EmailSender getSender() {
        return this.getSenderWithPriority(0);
    }

    public EmailSender getSenderWithPriority(int priority) {
        EmailSender sender = null;
        if (this.config != null && this.config.getString("type") != null) {
            String var3 = this.config.getString("type");
            byte var4 = -1;
            switch(var3.hashCode()) {
                case -2109773593:
                    if (var3.equals("SendInBlue")) {
                        var4 = 0;
                    }
                    break;
                case 2549334:
                    if (var3.equals("SMTP")) {
                        var4 = 2;
                    }
                    break;
                case 2137571039:
                    if (var3.equals("GoMail")) {
                        var4 = 1;
                    }
            }

            switch(var4) {
                case 0:
                    try {
                        sender = new SendInBlueSender(this.vertx, this.config);
                    } catch (URISyntaxException | InvalidConfigurationException var8) {
                        this.log.error(var8.getMessage(), var8);
                        this.vertx.close();
                    }
                    break;
                case 1:
                    try {
                        sender = new GoMailSender(this.vertx, this.config);
                    } catch (URISyntaxException | InvalidConfigurationException var7) {
                        this.log.error(var7.getMessage(), var7);
                        this.vertx.close();
                    }
                    break;
                case 2:
                    try {
                        sender = new SMTPSender(this.vertx, this.config);
                    } catch (InvalidConfigurationException var6) {
                        this.log.error(var6.getMessage(), var6);
                        this.vertx.close();
                    }
            }

            if (this.config.containsKey("postgresql")) {
                sender = new PostgresEmailSender((EmailSender)sender, this.vertx, this.moduleConfig, this.config, priority);
            }
        } else {
            sender = new SmtpSender(this.vertx);
        }

        return (EmailSender)sender;
    }
}
