# À propos de l'application LyStore
* Licence : [AGPL v3](http://www.gnu.org/licenses/agpl.txt) - Copyright Région Ile de France 
* Développeur : CGI
* Financeur : Région Ile de France
* Description : Service de gestion des achats

# Présentation du module

L'application **LyStore**, mise à disposition des lycées d'île-de-France, permet de gérer les différentes campagnes d'équipements issus de marchés publics en s'appuyant sur un catalogue. Elle propose deux types de campagnes :

 - Les campagnes basées sur un panier/commande et une enveloppe budgétaire annuelle allouée aux lycées
 - Les campagnes basées sur un panier/demande organisé sous forme d'une liste de voeux avec instruction de la demande

## Fonctionnalités

Deux profils utilisateurs sont disponibles :
 - Les personnels d'établissement responsables des équipements (typiquement les personnels administratifs)
 - Les personnels Région île-de-France, administrateurs de la plateforme

### Profil établissement

Pour ces utilisateurs, il est possible de :
 - Consulter les Campagnes d'équipements disponibles pour leur établissement
 - Consulter le catalogue d'équipements disponibles dans le cadre d'une Campagne
 - Formuler une demande d'équipements à partir d'un catalogue ou d'une saisie libre
 - Suivre l'avancement des commandes

### Profil administrateur

Pour ces utilisateurs, il est possible de :
 - Créer des campagnes d'équipements de type **cagnotte** ou de type **liste de voeux**
 - Gérer les accès aux campagnes
 - Créer un catalogue d'équipements avec fiches techniques détaillées
 - Ajouter pour chaque équipement du catalogue des options ou des prestations associées
 - Gérer les TVA
 - Gérer les périodes de garanties sur les équipements
 - Gérer l'obsolescence théorique des équipements
 - Associer les équipements saisis à des marchés publics
 - Gérer les marchés publics supports avec leurs titulaires et les correspondants Région
 - Organiser les équipements via un système de **tags**
 - Restreindre l'accès de certains équipements à des groupements d'établissements
 - Alimenter/gérer les enveloppes budgétaires des établissements tout au long de l'année
 - Instruire les demandes
 - Générer des bons de commande et des certificats de service fait pour les MAC
 - Créer des commandes régions
 - Gestion des Opérations
 - Gestion des rapports 
### Configuration
<pre>
{
  "config": {
    ...
    "iteration-worker": ${lystoreIterationWorker},
    ...
    "slack": {
        "token": "",
        "api-uri": "",
        "bot-username": "Lystore Bot",
        "channel": ""
    },
     "mail": {
        "enableMail": ${lystoreEnableMail},
        "domainMail": ${lystoreDomainMail},
        "notificationMail":${lystoreNotificationMail},
        "notificationHelpDeskMail":${lystoreNotificationHelpDeskMail},
        "notificationHelpDeskReceiver":${lystoreNotificationHelpDeskReceiver}
     },
     "node-pdf-generator" : {
     	"pdf-connector-id": "exportpdf",
     	"auth": "${nodePdfToken}",
     	"url" : "${nodePdfUri}"
     },
      "region-type-name": "${lystoreRegionTypeName}"
  }
}
</pre>

Dans votre springboard, vous devez inclure des variables d'environnement :
<pre>
lystoreIterationWorker = Integer
lystoreEnableMail = boolean
lystoreDomainMail = ${String}
lystoreNotificationMail = ${String}
lystoreNotificationHelpDeskMail = ${String}
lystoreNotificationHelpDeskReceiver = ${String}
lystoreRegionTypeName: ${String}
# External node pdf generator
nodePdfUri: ${String}
nodePdfToken: ${String}
</pre>


### postgresql 
Scripts sql à passer pour l'initialisation des bases sql sur de nouvelles plateformes 
```sql
# tax

INSERT INTO lystore.tax (id, name, value) VALUES (1, 'TVA', 20);
INSERT INTO lystore.tax (id, name, value) VALUES (6, 'Taxe 15.0%', 15.0);
SELECT pg_catalog.setval('lystore.tax_id_seq', 6, true);

#grade
 
INSERT INTO lystore.grade VALUES (1, 'AGRICULTURE');
INSERT INTO lystore.grade VALUES (2, 'ALIMENTATION');
INSERT INTO lystore.grade VALUES (3, 'AMENAGEMENT PAYSAGER');
INSERT INTO lystore.grade VALUES (4, 'AUDIOVISUEL');
INSERT INTO lystore.grade VALUES (5, 'AUTOMOBILE & ENGINS');
INSERT INTO lystore.grade VALUES (6, 'BTP');
INSERT INTO lystore.grade VALUES (7, 'CHIMIE');
INSERT INTO lystore.grade VALUES (8, 'COIFFURE-ESTHETIQUE');
INSERT INTO lystore.grade VALUES (9, 'COMMERCE');
INSERT INTO lystore.grade VALUES (10, 'COMMUNICATION');
INSERT INTO lystore.grade VALUES (11, 'EAU & PROPRETE');
INSERT INTO lystore.grade VALUES (12, 'ELECTRICITE & ELECTRONIQUE');
INSERT INTO lystore.grade VALUES (13, 'ENERGIE');
INSERT INTO lystore.grade VALUES (14, 'GENERAL');
INSERT INTO lystore.grade VALUES (15, 'HOTELLERIE RESTAURATION TOURISME');
INSERT INTO lystore.grade VALUES (16, 'INFORMATIQUE');
INSERT INTO lystore.grade VALUES (17, 'MAINTENANCE ET CONSTRUCTION AERONAUTIQUE');
INSERT INTO lystore.grade VALUES (18, 'MATERIAUX SOUPLES');
INSERT INTO lystore.grade VALUES (19, 'METIERS D’ART');
INSERT INTO lystore.grade VALUES (20, 'MLDS');
INSERT INTO lystore.grade VALUES (21, 'OPTIQUE');
INSERT INTO lystore.grade VALUES (22, 'PLASTURGIE');
INSERT INTO lystore.grade VALUES (23, 'PRODUCTIQUE');
INSERT INTO lystore.grade VALUES (24, 'SANITAIRE & SOCIAL');
INSERT INTO lystore.grade VALUES (25, 'SECURITE');
INSERT INTO lystore.grade VALUES (26, 'SPORT');
INSERT INTO lystore.grade VALUES (27, 'STRUCTURES METALLIQUES');
INSERT INTO lystore.grade VALUES (28, 'TERTIAIRE ADMINISTRATIF');
INSERT INTO lystore.grade VALUES (29, 'TRANSPORT LOGISTIQUE');
INSERT INTO lystore.grade VALUES (30, 'CPGE');
INSERT INTO lystore.grade VALUES (31, 'AGRICULTURE');
INSERT INTO lystore.grade VALUES (32, 'ALIMENTATION');
INSERT INTO lystore.grade VALUES (33, 'AMENAGEMENT PAYSAGER');
INSERT INTO lystore.grade VALUES (34, 'AUDIOVISUEL');
INSERT INTO lystore.grade VALUES (35, 'AUTOMOBILE & ENGINS');
INSERT INTO lystore.grade VALUES (36, 'BTP');
INSERT INTO lystore.grade VALUES (37, 'CHIMIE');
INSERT INTO lystore.grade VALUES (38, 'COIFFURE-ESTHETIQUE');
INSERT INTO lystore.grade VALUES (39, 'COMMERCE');
INSERT INTO lystore.grade VALUES (40, 'COMMUNICATION');
INSERT INTO lystore.grade VALUES (41, 'EAU & PROPRETE');
INSERT INTO lystore.grade VALUES (42, 'ELECTRICITE & ELECTRONIQUE');
INSERT INTO lystore.grade VALUES (43, 'ENERGIE');
INSERT INTO lystore.grade VALUES (44, 'GENERAL');
INSERT INTO lystore.grade VALUES (45, 'HOTELLERIE RESTAURATION TOURISME');
INSERT INTO lystore.grade VALUES (46, 'INFORMATIQUE');
INSERT INTO lystore.grade VALUES (47, 'MAINTENANCE ET CONSTRUCTION AERONAUTIQUE');
INSERT INTO lystore.grade VALUES (48, 'MATERIAUX SOUPLES');
INSERT INTO lystore.grade VALUES (49, 'METIERS D’ART');
INSERT INTO lystore.grade VALUES (50, 'MLDS');
INSERT INTO lystore.grade VALUES (51, 'OPTIQUE');
INSERT INTO lystore.grade VALUES (52, 'PLASTURGIE');
INSERT INTO lystore.grade VALUES (53, 'PRODUCTIQUE');
INSERT INTO lystore.grade VALUES (54, 'SANITAIRE & SOCIAL');
INSERT INTO lystore.grade VALUES (55, 'SECURITE');
INSERT INTO lystore.grade VALUES (56, 'SPORT');
INSERT INTO lystore.grade VALUES (57, 'STRUCTURES METALLIQUES');
INSERT INTO lystore.grade VALUES (58, 'TERTIAIRE ADMINISTRATIF');
INSERT INTO lystore.grade VALUES (59, 'TRANSPORT LOGISTIQUE');
INSERT INTO lystore.grade VALUES (60, 'CPGE');
 
SELECT pg_catalog.setval('lystore.grade_id_seq', 60, true);
```
