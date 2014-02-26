<#-- $This file is distributed under the terms of the license in /doc/license.txt$ -->

<div>

<section id="orcid-offer" role="region">
    <p>
    	Whoop-de-doo. Your ORCID ID '${orcid}' is confirmed and validated.
    </p>

	<#if addVivoId!false>
	    Your VIVO ID has been added to your ORCID record.
	</#if>
	
	<#if addCornellId!false>
	    Your Cornell NetID has been added to your ORCID record.
	</#if>

    <p>
         <a class="cancel" href="${continueUrl}" >Return to your profile page.</a>
    </p>
</section>

</div> 