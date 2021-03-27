# BharatAgriSmart CorDapp

This CorDapp demonstrates how a fungible assets like Farm produce can be brought into 
distributed ledger and leverage the digital contract for it. This prototype aims to 
replace the paper based contract farming agreement into smart contract in Corda DLT.

The Postman directory has a JSON collection to run the prototype and interact with nodes.

## CorDapp Components

### States
- `FarmProduceState`: is an `OwnableState` and `LinearState` representing a Farm Produce to which a farm agreement
can be proposed, accepted, settled or cancelled.
- `FarmAgreementState`: is a contract for the Farm Produce with a `LinearState`.
- `FarmAgreementStatus`: represents the actual status of the Farm Agreement. It is an enum class with values 
                        `IN_PROPOSAL`,`IN_CONTRACT`,`SETTLED` or `CANCELLED`.
### Contracts:
- `FarmProduceContract`: verifies the creation of Farm Produce. It has one command, `CreateFarmProduce`.

- `FarmAgreementContract`: governs the evolution of the Farm Agreement. It has the following commands to verify:
    - `ProposeFarmAgreement`: validates the rules governing Farm Agreement proposal.
    - `SettleAgreement`: validates the rules governing Farm Agreement settlement and changes its status to SETTLED from IN_CONTRACT.
    - `AcceptAgreement`: validates the Accept Farm agreement rules and changes the agreement status to IN_CONTRACT from IN_PROPOSAL.
    - `CancelAgreement`: validates Agreement cancellation rules and changes the status of an agreement.
### Flows:
- `CreateFarmProduceFlow`: is used to create a farm produce.
- `TransactionBroadcastFlow`: broadcasts the newly created a farm produce.
- `ObserveTransactionFlow`: is used to observe states from broadcast transaction.
- `ProposeFarmAgreementFlow`: is used to propose a Farm Agreement with Linear pointer pointing to the farm produce.
- `AcceptFarmAgreementFlow`: is used accept the proposed agreement.
- `AgreementSettlementFlow`: is used to settle the agreement with in-built Corda Cash.
    - `ChangeFarmAgreementStatusSettledFlow`: is an internal flow triggered after AgreementSettlement flows to change the agreement status.

## Pre-requisites:
All the steps to set up Corda on Desktop / Laptop is available under:
  https://docs.corda.net/getting-set-up.html
   - `Java 8 JDK`
   - `IntelliJ IDEA`
   - `Git`
   - `Gradle`
   - `Postman` can be downloaded from https://www.postman.com/downloads/
## Running the nodes:
Open a terminal and navigate to the project root directory.

To deploy nodes, type the following command:
```
./gradlew clean deployNodes
```
Wait until BUILD SUCCESSFUL. Then to run the nodes, navigate and type runnodes command as below:
```
./build/nodes/runnodes
```
JAR file for each node will be opened. Wait for the message Node for "NAME" started up and registered in xx.xx sec.
## Running the client webserver and Postman collection:
Open two more terminals to run the webserver of Farmer and Organic Market by executing the below command from the project root:

`./gradlew runOrganicMarketServer`

On second terminal

`./gradlew runFarmerAServer`

Please look for the status:
 Server$Companion.logStarted - Started Server.Companion in xx.xx seconds (JVM running for xx.xx)
 xx% EXECUTING [YYs] - The execution % can be ignored.
 
Open the Postman and import the collection file from projectroot/postman folder -  BharatAgriSmart.postman_collection.json
Each request can be executed by clicking on `Send` button.The responses can be seen on the same window. 
## Usage

Legal Prose, about filter, 
1. Navigate to project folder and run the Gradle task for deploying nodes, followed by runnodes.
may use the skip button if you wish to setup the deata yourself.

![Deploy Nodes](../screenshots/1DeployNodes.png)

![Run Nodes](screenshots/2RunNodes.png)

2. Once the nodes are fully started, run the webserver for each node in separate command prompt windows.
![Node Started](/screenshots/3NodeStarted.png)
![Run Webserver](screenshots/4Webserver.png)

3. After the webserver start, open the Postman to import the JSON collection to run the client.
![Postman] screenshots/5Postman.png)

4. The farm produce can be created with pre-filled values or new values can also be entered. After clicking on send,
response is shown on the same window.
![Create FarmProduce](screenshots/6CreateFarmProduce.png)

5. The farm produces can be filtered using name( if there are multiple offers from Farmers) or just all the available 
farm produces. The linear ID of the farm produce has to be copied to propose agreement on it.
![All FarmProduce] screenshots/7FarmProduceAll.png)
![Filter FarmProduce](screenshots/8Filter.png)

6. The Buyer issues cash using Corda in-built self issue cash. It should be more than minGuaranteed Price mentioned in
Farm Produce for agreement proposal.
![Issue Cash](screenshots/9IssueCash.png)

7. The Farm Agreement can be proposed with the copied Farm Produce Linear id. Make sure to add correct path for legal
prose attachment .zip file.
![Propose FarmAgreement](screenshots/10ProposeAgreement.png)

8. Farmer/Seller queries Farm Agreements and copies its Linear ID to accept the proposal.
![Query FarmAgreement](screenshots/11QueryFarmAgreement.png)
![Accept FarmAgreement](screenshots/12AcceptAgreement.png)

9. Status of an agreement changed to IN_CONTRACT from IN_PROPOSAL after acceptance can be seen.
![Status FarmAgreement](screenshots/13CheckAgreementInContract.png)

10. Farm Agreement settled with its Linear ID and Agreed amount.
![Settle FarmAgreement](screenshots/14SettleAgreement.png)

11. Status of an agreement changed to SETTLED from IN_CONTRACT.
![SettleStatus FarmAgreement](screenshots/15StatusSettled.png)

12. Ownership of farm produce is changed to buyer/organic market and agreed amount is transferred to seller/farmer.
There are two methods available in API to check farm produce states: 
To check all unconsumed states - ./farm-produce-states, 
To check all farm produce states - ./farm-produce-states-all
![Status FarmProduce](screenshots/16StatusFarmProduce.png)
![Status FarmProduceAll](screenshots/17StatusFarmProduceAll.png)
![Status Cash](screenshots/18CashTransfer.png)
It is assumed that negotiation between parties is happening off-chain.
As per the Indian Farm Laws, an active agreement can be cancelled anytime. This functionality can be tested by
proposing and accepting agreement. Then with Linear ID it can be cancelled.
