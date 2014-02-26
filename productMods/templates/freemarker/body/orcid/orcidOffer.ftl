<#-- $This file is distributed under the terms of the license in /doc/license.txt$ -->

<div>

<section id="orcid-offer" role="region">
    <p>Do you want to confirm your ORCID Identification?</p>
    
    <p>
    	If you click "Confirm", your browser will be redirected to the ORCID login page. 
    	Log in to your account, or create an account if you don't have one.
    	After logging in, authorize VIVO to read your profile.
    </p>

    <p>
    	If you click "Cancel", you will return to your profile page.
    </p>

    <form method="GET" action="${orcidControllerUrl}">
    	<label>
    		Would you like VIVO to add your VIVO account to your ORCID profile as an external identifier?
    		<input type="checkbox" name="addVivoId" />
    	</label>

		<#if mayAddCornellId??>
    	<label>
    		Would you like VIVO to add your Cornell NetID to your ORCID profile as an external identifier?
    		<input type="checkbox" name="addNetId" />
    	</label>
    	</#if>
    	
    	<p>
    		Note: If you check either box, ORCID will ask you to authorize the external identifier(s).
    	</p>

        <p><input type="submit" name="submit" value="Confirm" class="submit"/>
            or 
            <a class="cancel" href="${cancelUrl}" >Cancel</a>
        </p>
    </form>
</section>


</div> 