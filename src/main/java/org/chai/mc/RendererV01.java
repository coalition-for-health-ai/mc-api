package org.chai.mc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.chai.util.BibTeXUtil;
import org.chai.util.CommonMarkUtil;
import org.chai.util.IOUtil;
import org.chai.util.XMLUtil;
import org.jbibtex.ParseException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RendererV01 implements org.chai.mc.Renderer {

    public static String STAMP;
    static {
        try {
            STAMP = IOUtil.getResourceAsBase64("/stamp.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String renderBasicInfo(final Element basicInfo) {
        final String modelName = XMLUtil.getElementText(basicInfo, "ModelName");
        final String modelDeveloper = XMLUtil.getElementText(basicInfo, "ModelDeveloper");
        final String developerContact = XMLUtil.getElementText(basicInfo, "DeveloperContact");
        return String.format(
                """
                        <tr>
                            <td>
                                <div><b>Name:</b> %s</div>
                                <div><b>Developer:</b> %s</div>
                            </td>
                            <td>
                                <div><b>Inquires or to report an issue:</b> %s</div>
                            </td>
                        </tr>
                        """,
                CommonMarkUtil.markdownToHtml(modelName),
                CommonMarkUtil.markdownToHtml(modelDeveloper),
                CommonMarkUtil.markdownToHtml(developerContact));
    }

    public String renderReleaseInfo(final Element releaseInfo) {
        final String releaseStage = XMLUtil.getElementText(releaseInfo, "ReleaseStage");
        final String releaseDate = XMLUtil.getElementText(releaseInfo, "ReleaseDate");
        final String releaseVersion = XMLUtil.getElementText(releaseInfo, "ReleaseVersion");
        final String globalAvailability = XMLUtil.getElementText(releaseInfo, "GlobalAvailability");
        final String regulatoryApproval = XMLUtil.getElementTextWithDefault(releaseInfo, "RegulatoryApproval", "N/A");
        return String.format(
                """
                            <tr>
                                <td colspan="2">
                                    <ul style="
                                        list-style: none;
                                        padding-left: 0;
                                        margin-top: 0;
                                        margin-bottom: 0;
                                    ">
                                        <li><b>Release Stage:</b> %s</li>
                                        <li><b>Release Date:</b> %s</li>
                                        <li><b>Version:</b> %s</li>
                                        <li><b>Global Availability:</b> %s</li>
                                        <li><b>Regulatory Approval:</b> %s</li>
                                    </ul>
                                </td>
                            </tr>
                        """,
                CommonMarkUtil.markdownToHtml(releaseStage),
                CommonMarkUtil.markdownToHtml(releaseDate),
                CommonMarkUtil.markdownToHtml(releaseVersion),
                CommonMarkUtil.markdownToHtml(globalAvailability),
                CommonMarkUtil.markdownToHtml(regulatoryApproval));
    }

    public String renderModelSummary(final Element modelSummary) {
        final String summary = XMLUtil.getElementText(modelSummary, "Summary");
        final NodeList keywordsElements = modelSummary.getElementsByTagName("Keywords").item(0).getChildNodes();
        final List<String> keywords = new ArrayList<>(keywordsElements.getLength());
        for (int i = 0; i < keywordsElements.getLength(); i++) {
            keywords.add(keywordsElements.item(i).getTextContent());
        }
        return String.format(
                """
                        <td>
                            <div style="
                                display: flex;
                                flex-direction: column;
                                justify-content: space-between;
                                height: 100%%;
                            ">
                                <p style="margin-top: 0"><b>Summary:</b> %s</p>
                                <p style="margin-bottom: 0"><b>Keywords:</b> %s</p>
                            </div>
                        </td>
                        """,
                CommonMarkUtil.markdownToHtml(summary),
                keywords.stream().map(keyword -> CommonMarkUtil.markdownToHtml(keyword)).reduce((a, b) -> a + "; " + b)
                        .orElse("None"));
    }

    public String renderUsesAndDirections(final Element usesAndDirections) {
        final String intendedUseAndWorkflow = XMLUtil.getElementText(usesAndDirections, "IntendedUseAndWorkflow");
        final String primaryIntendedUsers = XMLUtil.getElementText(usesAndDirections, "PrimaryIntendedUsers");
        final String howToUse = XMLUtil.getElementText(usesAndDirections, "HowToUse");
        final String targetedPatientPopulation = XMLUtil.getElementText(usesAndDirections, "TargetedPatientPopulation");
        final String cautionedOutOfScopeSettings = XMLUtil.getElementText(usesAndDirections,
                "CautionedOutOfScopeSettings");
        return String.format("""
                <td style="vertical-align: top">
                    <b>Uses and Directions:</b>
                    <ul style="margin-top: 2px">
                        <li><b>Intended use and workflow:</b> %s</li>
                        <li><b>Primary intended users:</b> %s</li>
                        <li><b>How to use:</b> %s</li>
                        <li><b>Targeted patient population:</b> %s</li>
                        <li><b>Cautioned out-of-scope settings and use cases:</b> %s</li>
                    </ul>
                </td>
                """, CommonMarkUtil.markdownToHtml(intendedUseAndWorkflow),
                CommonMarkUtil.markdownToHtml(primaryIntendedUsers),
                CommonMarkUtil.markdownToHtml(howToUse),
                CommonMarkUtil.markdownToHtml(targetedPatientPopulation),
                CommonMarkUtil.markdownToHtml(cautionedOutOfScopeSettings));
    }

    public String renderWarnings(final Element warnings) {
        final String knownRisksAndLimitations = XMLUtil.getElementText(warnings, "KnownRisksAndLimitations");
        final String knownBiasesOrEthicalConsiderations = XMLUtil.getElementText(warnings,
                "KnownBiasesOrEthicalConsiderations");
        final String clinicalRiskLevel = XMLUtil.getElementText(warnings, "ClinicalRiskLevel");
        return String.format("""
                <tr style="background-color: black; color: white">
                    <td colspan="2">
                        <b>Warnings</b>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">
                        <ul style="margin-top: 4px; margin-bottom: 4px">
                            <li><b>Known risks and limitations:</b> %s</li>
                            <li><b>Known biases or ethical considerations:</b> %s</li>
                            <li><b>Clinical risk level:</b> %s</li>
                        </ul>
                    </td>
                </tr>
                """,
                CommonMarkUtil.markdownToHtml(knownRisksAndLimitations),
                CommonMarkUtil.markdownToHtml(knownBiasesOrEthicalConsiderations),
                CommonMarkUtil.markdownToHtml(clinicalRiskLevel));
    }

    public String renderTrustIngredients(final Element trustIngredients) {
        final Element aiSystemFacts = XMLUtil.getElement(trustIngredients, "AISystemFacts");
        final String outcomesAndOutputs = XMLUtil.getElementText(aiSystemFacts, "OutcomesAndOutputs");
        final String modelType = XMLUtil.getElementText(aiSystemFacts, "ModelType");
        final String foundationModels = XMLUtil.getElementTextWithDefault(aiSystemFacts, "FoundationModels", "N/A");
        final String inputDataSource = XMLUtil.getElementText(aiSystemFacts, "InputDataSource");
        final String outputAndInputDataTypes = XMLUtil.getElementText(aiSystemFacts, "OutputAndInputDataTypes");
        final String developmentDataCharacterization = XMLUtil.getElementText(aiSystemFacts,
                "DevelopmentDataCharacterization");
        final String biasMitigationApproaches = XMLUtil.getElementText(aiSystemFacts, "BiasMitigationApproaches");
        final String ongoingMaintenance = XMLUtil.getElementText(aiSystemFacts, "OngoingMaintenance");
        final String security = XMLUtil.getElementTextWithDefault(aiSystemFacts, "Security", "N/A");
        final String transparency = XMLUtil.getElementTextWithDefault(aiSystemFacts, "Transparency", "N/A");
        final Element transparencyInformation = XMLUtil.getElement(trustIngredients, "TransparencyInformation");
        final String fundingSource = XMLUtil.getElementText(transparencyInformation, "FundingSource");
        final String thirdPartyInformation = XMLUtil.getElementText(transparencyInformation, "ThirdPartyInformation");
        final String stakeholdersConsulted = XMLUtil.getElementText(transparencyInformation, "StakeholdersConsulted");
        return String.format(
                """
                        <tr style="background-color: black; color: white">
                            <td colspan="2">
                                <b>Trust Ingredients</b>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2">
                                <b>AI System Facts:</b>
                                <ul style="margin-top: 4px; margin-bottom: 4px">
                                    <li><b>Outcome(s) and output(s):</b> %s</li>
                                    <li><b>Model type:</b> %s</li>
                                    <li><b>Foundation models used in application:</b> %s</li>
                                    <li><b>Input data source:</b> %s</li>
                                    <li><b>Output/Input data type:</b> %s</li>
                                    <li><b>Development data characterization:</b> %s</li>
                                    <li><b>Bias mitigation approaches:</b> %s</li>
                                    <li><b>Ongoing Maintenance:</b> %s</li>
                                    <li><b>Security and compliance environment practices or accreditations:</b> %s</li>
                                    <li><b>Transparency, Intelligibility, and Accountability mechanisms:</b> %s</li>
                                </ul>
                                <b>Transparency Information:</b>
                                <ul style="margin-top: 4px; margin-bottom: 4px">
                                    <li><b>Funding source of the technical implementation:</b> %s</li>
                                    <li><b>3rd Party Information:</b> %s</li>
                                    <li><b>Stakeholders consulted during design of intervention (e.g. patients, providers):</b> %s</li>
                                </ul>
                            </td>
                        </tr>
                        """,
                CommonMarkUtil.markdownToHtml(outcomesAndOutputs),
                CommonMarkUtil.markdownToHtml(modelType),
                CommonMarkUtil.markdownToHtml(foundationModels),
                CommonMarkUtil.markdownToHtml(inputDataSource),
                CommonMarkUtil.markdownToHtml(outputAndInputDataTypes),
                CommonMarkUtil.markdownToHtml(developmentDataCharacterization),
                CommonMarkUtil.markdownToHtml(biasMitigationApproaches),
                CommonMarkUtil.markdownToHtml(ongoingMaintenance),
                CommonMarkUtil.markdownToHtml(security),
                CommonMarkUtil.markdownToHtml(transparency),
                CommonMarkUtil.markdownToHtml(fundingSource),
                CommonMarkUtil.markdownToHtml(thirdPartyInformation),
                CommonMarkUtil.markdownToHtml(stakeholdersConsulted));
    }

    public String renderKeyMetrics(final Element keyMetrics) {
        final Element usefulnessUsabilityEfficacy = XMLUtil.getElement(keyMetrics, "UsefulnessUsabilityEfficacy");
        final String metricGoalUsefulnessUsabilityEfficacy = XMLUtil.getElementText(usefulnessUsabilityEfficacy,
                "MetricGoal");
        final String resultUsefulnessUsabilityEfficacy = XMLUtil.getElementText(usefulnessUsabilityEfficacy, "Result");
        final String interpretationUsefulnessUsabilityEfficacy = XMLUtil.getElementText(usefulnessUsabilityEfficacy,
                "Interpretation");
        final String testTypeUsefulnessUsabilityEfficacy = XMLUtil.getElementText(usefulnessUsabilityEfficacy,
                "TestType");
        final String tesstingDataDescriptionUsefulnessUsabilityEfficacy = XMLUtil
                .getElementText(usefulnessUsabilityEfficacy, "TestingDataDescription");
        final String validationProcessUsefulnessUsabilityEfficacy = XMLUtil.getElementText(
                usefulnessUsabilityEfficacy, "ValidationProcessAndJustification");
        final Element fairnessEquity = XMLUtil.getElement(keyMetrics, "FairnessEquity");
        final String metricGoalFairnessEquity = XMLUtil.getElementText(fairnessEquity,
                "MetricGoal");
        final String resultFairnessEquity = XMLUtil.getElementText(fairnessEquity, "Result");
        final String interpretationFairnessEquity = XMLUtil.getElementText(fairnessEquity,
                "Interpretation");
        final String testTypeFairnessEquity = XMLUtil.getElementText(fairnessEquity,
                "TestType");
        final String tesstingDataDescriptionFairnessEquity = XMLUtil.getElementText(fairnessEquity,
                "TestingDataDescription");
        final String validationProcessFairnessEquity = XMLUtil.getElementText(
                fairnessEquity, "ValidationProcessAndJustification");
        final Element safetyReliability = XMLUtil.getElement(keyMetrics, "SafetyReliability");
        final String metricGoalSafetyReliability = XMLUtil.getElementText(safetyReliability,
                "MetricGoal");
        final String resultSafetyReliability = XMLUtil.getElementText(safetyReliability, "Result");
        final String interpretationSafetyReliability = XMLUtil.getElementText(safetyReliability,
                "Interpretation");
        final String testTypeSafetyReliability = XMLUtil.getElementText(safetyReliability,
                "TestType");
        final String tesstingDataDescriptionSafetyReliability = XMLUtil.getElementText(safetyReliability,
                "TestingDataDescription");
        final String validationProcessSafetyReliability = XMLUtil.getElementText(safetyReliability,
                "ValidationProcessAndJustification");
        return String.format("""
                <tr style="background-color: black; color: white">
                    <td colspan="2">
                        <b>Key Metrics</b>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">
                        <table style="font-size: 14px">
                            <colgroup>
                                <col style="background-color: #dce9f5" span="2">
                                <col style="background-color: #f5e3d7" span="2">
                                <col style="background-color: #def1d3" span="2">
                            </colgroup>
                            <tbody style="vertical-align: top">
                                <tr style="text-align: center">
                                    <th colspan="2">Usefulness, Usability, and Efficacy</th>
                                    <th colspan="2">Fairness and Equity</th>
                                    <th colspan="2">Safety and Reliability</th>
                                </tr>
                                <tr>
                                    <td colspan="2"><b>Goal of metric:</b> %s</td>
                                    <td colspan="2"><b>Goal of metric:</b> %s</td>
                                    <td colspan="2"><b>Goal of metric:</b> %s</td>
                                </tr>
                                <tr>
                                    <td><b>Result: </b>%s</td>
                                    <td><b>Interpretation: </b>%s</td>
                                    <td><b>Result: </b>%s</td>
                                    <td><b>Interpretation: </b>%s</td>
                                    <td><b>Result: </b>%s</td>
                                    <td><b>Interpretation: </b>%s</td>
                                </tr>
                                <tr>
                                    <td colspan="2"><b>Test Type:</b> %s</td>
                                    <td colspan="2"><b>Test Type:</b> %s</td>
                                    <td colspan="2"><b>Test Type:</b> %s</td>
                                </tr>
                                <tr>
                                    <td colspan="2"><b>Testing Data Description:</b> %s</td>
                                    <td colspan="2"><b>Testing Data Description:</b> %s</td>
                                    <td colspan="2"><b>Testing Data Description:</b> %s</td>
                                </tr>
                                <tr>
                                    <td colspan="2"><b>Validation Process and Justification:</b> %s</td>
                                    <td colspan="2"><b>Validation Process and Justification:</b> %s</td>
                                    <td colspan="2"><b>Validation Process and Justification:</b> %s</td>
                                </tr>
                            </tbody>
                        </table>
                    </td>
                </tr>
                """,
                CommonMarkUtil.markdownToHtml(metricGoalUsefulnessUsabilityEfficacy),
                CommonMarkUtil.markdownToHtml(metricGoalFairnessEquity),
                CommonMarkUtil.markdownToHtml(metricGoalSafetyReliability),
                CommonMarkUtil.markdownToHtml(resultUsefulnessUsabilityEfficacy),
                CommonMarkUtil.markdownToHtml(interpretationUsefulnessUsabilityEfficacy),
                CommonMarkUtil.markdownToHtml(resultFairnessEquity),
                CommonMarkUtil.markdownToHtml(interpretationFairnessEquity),
                CommonMarkUtil.markdownToHtml(resultSafetyReliability),
                CommonMarkUtil.markdownToHtml(interpretationSafetyReliability),
                CommonMarkUtil.markdownToHtml(testTypeUsefulnessUsabilityEfficacy),
                CommonMarkUtil.markdownToHtml(testTypeFairnessEquity),
                CommonMarkUtil.markdownToHtml(testTypeSafetyReliability),
                CommonMarkUtil.markdownToHtml(tesstingDataDescriptionUsefulnessUsabilityEfficacy),
                CommonMarkUtil.markdownToHtml(tesstingDataDescriptionFairnessEquity),
                CommonMarkUtil.markdownToHtml(tesstingDataDescriptionSafetyReliability),
                CommonMarkUtil.markdownToHtml(validationProcessUsefulnessUsabilityEfficacy),
                CommonMarkUtil.markdownToHtml(validationProcessFairnessEquity),
                CommonMarkUtil.markdownToHtml(validationProcessSafetyReliability));
    }

    public String renderResources(final Element resources) {
        final String evaluationReferences = XMLUtil.getElementTextWithDefault(resources, "EvaluationReferences", "N/A");
        final String clinicalTrial = XMLUtil.getElementTextWithDefault(resources, "ClinicalTrial", "N/A");
        final String peerReviewedPublications = XMLUtil.getElementText(resources, "PeerReviewedPublications");
        final String reimbursementStatus = XMLUtil.getElementTextWithDefault(resources, "ReimbursementStatus", "N/A");
        final String patientConsentOrDisclosure = XMLUtil.getElementText(resources, "PatientConsentOrDisclosure");
        return String.format("""
                <tr style="background-color: black; color: white">
                    <td colspan="2">
                        <b>Resources</b>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">
                        <ul style="margin-top: 4px; margin-bottom: 4px">
                            <li><b>Evalation References:</b> %s</li>
                            <li><b>Clinical Trial:</b> %s</li>
                            <li><b>Peer Reviewed Publication(s):</b> %s</li>
                            <li><b>Reimbursement Status:</b> %s</li>
                            <li><b>Patient consent or disclosure required or suggested:</b> %s</li>
                        </ul>
                    </td>
                </tr>
                """, CommonMarkUtil.markdownToHtml(evaluationReferences),
                CommonMarkUtil.markdownToHtml(clinicalTrial),
                CommonMarkUtil.markdownToHtml(peerReviewedPublications),
                CommonMarkUtil.markdownToHtml(reimbursementStatus),
                CommonMarkUtil.markdownToHtml(patientConsentOrDisclosure));
    }

    public String renderReferences(final Element bibliography) throws ParseException, IOException {
        return BibTeXUtil.bibtexToHtml(bibliography.getTextContent());
    }

    public String renderStamp(final Node signature) {
        if (signature == null) {
            return "";
        } else {
            return String.format("""
                    <img src="data:image/png;charset=utf-8;base64,%s" style="
                        display: inline-block;
                        height: 4em;
                        float: left;
                        margin-right: 1em;
                    ">
                    <p>CHAI has verified this model card to be in alignment with
                    instructions set forth by CHAI while demonstrating
                    sufficient rigorous and detail.</p>
                    <hr>
                    """, STAMP);
        }
    }

    @Override
    public String render(final Element appliedModelCard) {
        try {
            return String.format(
                    """
                            <!DOCTYPE html>
                            <html dir="ltr" lang="en">
                                <head>
                                    <meta charset="UTF-8">
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/modern-normalize@3.0.1/modern-normalize.min.css">
                                    <link rel="preconnect" href="https://fonts.googleapis.com">
                                    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                                    <link href="https://fonts.googleapis.com/css2?family=Roboto:ital,wght@0,100..900;1,100..900&display=swap" rel="stylesheet">
                                    <style>
                                        :root {
                                            print-color-adjust: exact;
                                            -webkit-print-color-adjust: exact;
                                            font-family: "Roboto", sans-serif;
                                            font-optical-sizing: auto;
                                            font-style: normal;
                                            word-wrap: break-word;
                                        }

                                        table {
                                            width: 100%%;
                                            height: 1px; /* magic number for height inheritance (ModelSummary) */
                                            border: 2px solid black;
                                            border-collapse: collapse;
                                            text-align: left;
                                            table-layout: fixed;
                                        }
                                        tr, th, td {
                                            border: 2px solid black;
                                            padding: 4px;
                                        }

                                        .csl-entry {
                                            padding-bottom: 1em;
                                            text-indent: -0.5in;
                                            padding-left: 0.5in;
                                        }
                                    </style>
                                    <title>CHAI Applied Model Card</title>
                                </head>
                                <body>
                                    <p>Applied Model Card v0.1</p>
                                    <h1 style="text-align: center; margin-top: 0">CHAI Applied Model Card</h1>
                                    <table>
                                        <tbody>
                                            %s
                                            %s
                                            <tr>
                                                %s
                                                %s
                                            </tr>
                                            %s
                                            %s
                                            %s
                                            %s
                                        </tbody>
                                    </table>

                                    <h2>References</h2>
                                    %s
                                    <hr>

                                    %s

                                    <p>Note: The mention or sharing of any examples, products, organizations, or individuals does not indicate any endorsement of those examples, products, organizations, or individuals by the Coalition for Health AI (CHAI). Any examples provided here are still under review for alignment with existing standards and instructions. We welcome feedback and stress-testing of the tool in draft form. </p>

                                    <p>The information provided in this document is for general informational purposes only and does not constitute legal advice. It is not intended to create, and receipt or review of it does not establish, an attorney-client relationship. </p>

                                    <p>This document should not be relied upon as a substitute for consulting with qualified legal or compliance professionals. Organizations and individuals are encouraged to seek advice specific to their unique circumstances to ensure adherence to applicable laws, regulations, and standards. </p>

                                    <p>For instructions, references, contributors, and disclaimers please refer to the full documentation located at <a href="https://www.chai.org/">www.chai.org</a>. </p>

                                    <p>Copyright (c) 2024 Coalition for Health AI, Inc. </p>
                                </body>
                            </html>
                            """,
                    renderBasicInfo(XMLUtil.getElement(appliedModelCard, "BasicInfo")),
                    renderReleaseInfo(XMLUtil.getElement(appliedModelCard, "ReleaseInfo")),
                    renderModelSummary(XMLUtil.getElement(appliedModelCard, "ModelSummary")),
                    renderUsesAndDirections(XMLUtil.getElement(appliedModelCard, "UsesAndDirections")),
                    renderWarnings(XMLUtil.getElement(appliedModelCard, "Warnings")),
                    renderTrustIngredients(XMLUtil.getElement(appliedModelCard, "TrustIngredients")),
                    renderKeyMetrics(XMLUtil.getElement(appliedModelCard, "KeyMetrics")),
                    renderResources(XMLUtil.getElement(appliedModelCard, "Resources")),
                    renderReferences(XMLUtil.getElement(appliedModelCard, "Bibliography")),
                    renderStamp(XMLUtil.getXmlSignatureNode(appliedModelCard)));
        } catch (ParseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
