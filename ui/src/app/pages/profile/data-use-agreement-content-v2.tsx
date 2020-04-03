import {StyledAnchorTag} from 'app/components/buttons';
import {BolderHeader, BoldHeader} from 'app/components/headers';
import {AouTitle} from 'app/components/text-wrappers';
import * as React from 'react';
import {
  IndentedListItem,
  IndentedOrderedList,
  IndentedUnorderedList,
  SecondHeader
} from './data-user-code-of-conduct-styles';

const AOU_DEFINITION_URL = 'https://allofus.nih.gov/about/about-all-us-research-program';
const CORE_VALUES_URL = 'https://allofus.nih.gov/about/core-values';
const MARKETING_URL =
  'https://www.govinfo.gov/content/pkg/CFR-2004-title45-vol1/pdf/CFR-2004-title45-vol1-sec164-501.pdf';
const PII_URL = 'https://www.govinfo.gov/content/pkg/CFR-2014-title2-vol1/pdf/CFR-2014-title2-vol1-sec200-79.pdf';
const STIGMATIZING_RESEARCH_URL =
  'https://docs.google.com/document/d/1RGWCmujvunLRdlBTYpZArcben4QNFwOhp7mSDK3puC0/view';

const styles = {
  dataUseParagraphStyles: {
    marginTop: '0.5rem'
  }
};

{/* NOTE: Make sure to update dataUseAgreementVersion if there is any change to the DUA text. */}
export const DataUseAgreementContentV2 = () => {
  return <div style={styles.dataUseParagraphStyles}>
    <BolderHeader style={{display: 'flex', justifyContent: 'center'}}><AouTitle/></BolderHeader>
    <BoldHeader  style={{display: 'flex', justifyContent: 'center'}}>Demonstration Project Data Use Agreement</BoldHeader>
    <div style={styles.dataUseParagraphStyles}>
      This data use agreement describes how <AouTitle/> data can and cannot be used for the purposes
      of program-approved demonstration projects.
    </div>
    <div style={styles.dataUseParagraphStyles}>
      This is an agreement between Vanderbilt University Medical Center and authorized demonstration users of data
      from
      the <AouTitle/>.
    </div>
    <div style={styles.dataUseParagraphStyles}>
      An <strong>authorized demonstration user</strong> is a person who is authorized to access
      and/or work with registered or
      controlled tier data from the <AouTitle/> for the exclusive purpose of a program-approved
      demonstration project. Authorized demonstration users are limited to trainees, faculty or staff
      at <AouTitle/> consortium partner institutions.
    </div>
    <div style={styles.dataUseParagraphStyles}>
      <strong>Before</strong> they access and/or work with <AouTitle/> data, authorized demonstration users must:
      <IndentedOrderedList>
        <IndentedListItem>complete the <AouTitle/> research ethics training; and</IndentedListItem>
        <IndentedListItem>read and attest to this data use agreement</IndentedListItem>
      </IndentedOrderedList>
    </div>
    <div style={styles.dataUseParagraphStyles}>Please read this agreement carefully and completely before signing.</div>
    <SecondHeader>As an “Authorized Demonstration User” of the <AouTitle/> data, I
      will:</SecondHeader>
    <IndentedUnorderedList>
      <IndentedListItem>read and adhere to the <AouTitle/> <StyledAnchorTag target='_blank' href={CORE_VALUES_URL}>core
        values</StyledAnchorTag>.
      </IndentedListItem>
      <IndentedListItem>know and follow all laws regarding research involving human data and data privacy that are
        applicable in the area where I am conducting research.
        <IndentedUnorderedList>
          <IndentedListItem>In the US, this includes all applicable federal, state, and local laws.</IndentedListItem>
          <IndentedListItem>Outside of the US, other laws will apply.</IndentedListItem>
        </IndentedUnorderedList>
      </IndentedListItem>
      <IndentedListItem>respect the privacy of research participants at all times.
        <IndentedUnorderedList>
          <IndentedListItem>
            I will <strong>NOT</strong> use or disclose any
            information that directly identifies one or more participants.
            <IndentedUnorderedList>
              <IndentedListItem>
                If I become aware of any information that directly identifies one or more
                participants, I will notify the <AouTitle/> immediately
                using the appropriate process.
              </IndentedListItem>
            </IndentedUnorderedList>
          </IndentedListItem>
          <IndentedListItem>
            I will <strong>NOT</strong> attempt to re-identify research
            participants or their relatives.
            <IndentedUnorderedList>
              <IndentedListItem>
                If I unintentionally re-identify participants through the process of my work, I will
                contact the <AouTitle/> immediately using the appropriate process.
              </IndentedListItem>
            </IndentedUnorderedList>
          </IndentedListItem>
          <IndentedListItem>
            If I become aware of any uses or disclosures of <AouTitle/> data that
            could endanger the security or privacy of research participants, I will contact
            the <AouTitle/> immediately using the appropriate process.
          </IndentedListItem>
        </IndentedUnorderedList>
      </IndentedListItem>
      <IndentedListItem>provide a meaningful description of my research purpose when establishing
        my <AouTitle/> workspace.</IndentedListItem>
      <IndentedUnorderedList>
        <IndentedListItem>This description will accurately reflect the demonstration project proposal for which I
          received <i>All of Us</i> approval.</IndentedListItem>
        <IndentedListItem>Within my workspace, I will only use the data for the demonstration project for which I have
          received <i>All of Us</i> approval.
        </IndentedListItem>
      </IndentedUnorderedList>
      <IndentedListItem>take full responsibility for any external data, files, or software that I import into my
        Workspace and the consequences thereof.</IndentedListItem>
      <IndentedUnorderedList>
        <IndentedListItem>I will know and follow all applicable laws, regulations, and policies regarding access and
          use for any external data, files, or software that I upload into my Workspace.</IndentedListItem>
        <IndentedListItem>I will <strong>NOT</strong> upload data or files containing
          personally identifiable information (PII).
          <IndentedUnorderedList><IndentedListItem>
            PII means information that can be used to distinguish or trace the identity of an individual
            (e.g., name, social security number, biometric records, etc.) either alone, or when combined with other
            personal or identifying information that is linked or linkable to a specific
            individual <StyledAnchorTag href={PII_URL}>(2 CFR §200.79)</StyledAnchorTag>
          </IndentedListItem></IndentedUnorderedList>
        </IndentedListItem>
        <IndentedListItem>I will use any external data, files, or software that I upload into my Workspace
          exclusively for the research purpose I have provided for that Workspace.</IndentedListItem>
        <IndentedListItem>I will <strong>NOT</strong> use any external data, files, or software that I upload into
          my Workspace for any malicious purposes.</IndentedListItem>
        <IndentedListItem>If any import of data, files, or software into my Workspace results in unforeseen
          consequences and/or unintentional violation of these terms, I will notify
          the <AouTitle/> as soon as I become aware using the appropriate process.</IndentedListItem>
      </IndentedUnorderedList>
      <IndentedListItem>use a version of the <AouTitle/> database that is current at or after the time my
        analysis begins.</IndentedListItem>
      <IndentedUnorderedList>
        <IndentedListItem>Archived versions of the database are maintained for the sole purpose of completion of
          existing studies or replication of previous studies. New work may not be initiated on
          archived versions of the database.
        </IndentedListItem>
      </IndentedUnorderedList>
      <IndentedListItem>
        share the results of my demonstration project and all contents of my Workbench with the <AouTitle/>.
      </IndentedListItem>
      <IndentedUnorderedList>
        <IndentedListItem>
          My workbench and its contents may be made public for the benefit of all authorized users.
        </IndentedListItem>
      </IndentedUnorderedList>
      <IndentedListItem>honor the contribution to my work of those who take part in <i>All of Us</i>
        <IndentedUnorderedList>
          <IndentedListItem>I will acknowledge the <AouTitle/> and its research participants
            in all oral and written presentations, disclosures, and publications resulting from
            any analyses of the data.
            <IndentedUnorderedList>
              <IndentedListItem style={{margin: '0.5rem 0'}}>Here is an example acknowledgement statement:
                <br/>
                “The <AouTitle/> is supported by the National Institutes of Health, Office of the
                Director: Regional Medical Centers: 1 OT2 OD026549; 1 OT2 OD026554; 1 OT2 OD026557; 1 OT2 OD026556;
                1 OT2 OD026550; 1 OT2 OD 026552; 1 OT2 OD026553; 1 OT2 OD026548; 1 OT2 OD026551; 1 OT2 OD026555;
                IAA #: AOD 16037; Federally Qualified Health Centers: HHSN 263201600085U; Data and Research Center:
                5 U2C OD023196; Biobank: 1 U24 OD023121; The Participant Center: U24 OD023176; Participant Technology
                Systems Center: 1 U24 OD023163; Communications and Engagement: 3 OT2 OD023205; 3 OT2 OD023206;
                and Community Partners: 1 OT2 OD025277; 3 OT2 OD025315; 1 OT2 OD025337; 1 OT2 OD025276. In addition,
                the <AouTitle/> would not be possible without the partnership of its participants.”
              </IndentedListItem>
              <IndentedListItem>
                I will submit an electronic version of a final, peer-reviewed manuscript to PubMed Central
                immediately upon acceptance for publication, to be made publicly available no later than 12
                months after the official date of publication.
              </IndentedListItem>
            </IndentedUnorderedList>
          </IndentedListItem>
        </IndentedUnorderedList>
      </IndentedListItem>
    </IndentedUnorderedList>
    <SecondHeader>As “Authorized Demonstration User” of the <AouTitle/> data, I
      will:</SecondHeader>
    <IndentedUnorderedList>
      <IndentedListItem><strong>NOT</strong> share my login information with anyone.
        <IndentedUnorderedList>
          <IndentedListItem>I will not share my login information with another authorized demonstration or other user
            of the <AouTitle/>.
          </IndentedListItem>
          <IndentedListItem>I will not create any group or shared accounts.</IndentedListItem>
        </IndentedUnorderedList>
      </IndentedListItem>
      <IndentedListItem>
        <strong>NOT</strong> use <AouTitle/> data or any external data, files, or software that I upload
        into the Research Workbench for research that is discriminatory or stigmatizing of individuals, families,
        groups, or communities. Please review the <i>All of Us</i> policy on stigmatizing
        research <StyledAnchorTag href={STIGMATIZING_RESEARCH_URL} target='_blank'>here</StyledAnchorTag>.
        <IndentedUnorderedList>
          <IndentedListItem>I will contact the <AouTitle/> Resource Access Board (RAB) for
            further guidance on this point as needed.
          </IndentedListItem>
        </IndentedUnorderedList>
      </IndentedListItem>
      <IndentedListItem><strong>NOT</strong> attempt to contact <AouTitle/> participants.
      </IndentedListItem>
      <IndentedListItem><strong>NOT</strong> make copies of or download participant-level data and remove it from
        the <AouTitle/> environment.
        <IndentedUnorderedList>
          <IndentedListItem>I will not take screenshots or attempt any other way of
            copying participant-level data.
          </IndentedListItem>
        </IndentedUnorderedList>
      </IndentedListItem>
      <IndentedListItem><strong>NOT</strong> redistribute or publish registered or controlled tier <AouTitle/> data
        including aggregate statistics that are more granular than buckets of 20 individuals without explicit approval
        from the <AouTitle/>.
      </IndentedListItem>
      <IndentedListItem><strong>NOT</strong> sell or distribute <AouTitle/> data at any level of granularity for the
        purpose of profit or monetary gains.</IndentedListItem>
      <IndentedListItem><strong>NOT</strong> attempt to link participant-level <i>All of Us</i> data from the
        registered or controlled tier with participant-level data from other sources without explicit permission from
        the <AouTitle/>.</IndentedListItem>
      <IndentedListItem>
        <strong>NOT</strong> use <AouTitle/> data or any part of the Research Hub for marketing purposes.
        <IndentedUnorderedList>
          <IndentedListItem>“Marketing” means a communication about a product or service that encourages recipients of
            the communication to purchase or use the product or service (<StyledAnchorTag
              href={MARKETING_URL}>US 45 CFR 164.501</StyledAnchorTag>).
          </IndentedListItem>
        </IndentedUnorderedList>
      </IndentedListItem>
      <IndentedListItem><strong>NOT</strong> represent that the <AouTitle/> endorses or approves of my research unless
        such endorsement is expressly provided.
      </IndentedListItem>
    </IndentedUnorderedList>

    <SecondHeader>Terms and Definitions:</SecondHeader>
    <IndentedUnorderedList>
      <IndentedListItem>The <StyledAnchorTag href={AOU_DEFINITION_URL} target='_blank'><AouTitle/></StyledAnchorTag> is
        a national longitudinal research initiative that aims to engage one million or more participants living in the
        United States. Participants contribute health data and specimens (blood, urine, saliva) to a repository that
        includes health, behavioral, genomic, and other data. The <AouTitle/> is a key component of the Precision
        Medicine Initiative, which aims to leverage advances in genomics and health information technology to
        accelerate biomedical discoveries.</IndentedListItem>
      <IndentedListItem>
        There are <strong>three data access tiers</strong> within the <AouTitle/>.
        <IndentedUnorderedList>
          <IndentedListItem><strong>Public tier:</strong> The resource tier containing only summary statistics and
            aggregate information that poses negligible risks to the privacy of research participants; the Public Tier
            can be accessed by anyone without logging into the <i>All of Us</i> Research Platform.</IndentedListItem>
          <IndentedListItem><strong>Registered tier:</strong> The resource tier that contains data elements that have a
            lower risk of unapproved re-identification, thus carries minimal risk to the privacy of research
            participants; Registered data can only be accessed after logging into the <i>All of Us</i> Research
            Platform; all access will be logged and may be audited.</IndentedListItem>
          <IndentedListItem><strong>Controlled tier:</strong> The resource tier that contains data elements that may
            not, in their own right, readily identify individual participants, but may increase the risk of unapproved
            re-identification when combined with other data elements; such data includes individual-level genomic data,
            clinical notes, and narrative data; users must be appropriately accredited and granted approval to access
            the Controlled Tier, and all access will be logged and may be audited.
          </IndentedListItem>
        </IndentedUnorderedList>
      </IndentedListItem>
      <IndentedListItem>
        An <strong>authorized demonstration user</strong> is a person who is authorized to access and/or work
        with <strong>registered</strong> or <strong>controlled</strong> tier data from the <AouTitle/> for the
        exclusive purpose of conducting demonstration project(s) sanctioned by the program. Authorized
        demonstration users must complete the <AouTitle/> research ethics training, attest to this agreement,
        and restrict their activities, under this agreement, to those tasks related to the approved demonstration
        project.
      </IndentedListItem>
      <IndentedListItem>
        The <strong>Committee on Access, Privacy, and Security (CAPS)</strong> is the committee that directs the
        policies and implementation of data access for the <AouTitle/>; CAPS is overseen by
        the <i>All of Us</i> Steering Committee.
      </IndentedListItem>
      <IndentedListItem>The <strong>Resource Access Board (RAB)</strong> is the board that operationalizes decisions
        regarding data access, with review by the CAPS; responsibilities include: administration of user registration
        and approval, review of potentially stigmatizing research proposals, and review of potential violations of
        data access principles; the RAB reports to CAPS and the <i>All of Us</i> Steering Committee. </IndentedListItem>
      <IndentedListItem>
        <strong>Workspace</strong>: A user-created analytical sandbox within the research platform where users can
        virtually pull in subsets of data from the <AouTitle/> database and perform analyses; authorized demonstration
        users must create a new workspace for each demonstration project using <i>All of Us</i> data and provide a
        plain language description of the demonstration project, as well as other project information, that may be
        published publicly on an <i>All of Us</i> website.
      </IndentedListItem>
    </IndentedUnorderedList>

    <SecondHeader>Data Disclaimer:</SecondHeader>
    <p>The <AouTitle/> does not guarantee the accuracy of the data in
      the <AouTitle/> database. The <AouTitle/> does
      not guarantee the performance of the software in the <AouTitle/> database.
      The <AouTitle/> does not warrant or endorse
      the research results obtained by using the <i>All of Us</i> database.
    </p>
  </div>;
};
