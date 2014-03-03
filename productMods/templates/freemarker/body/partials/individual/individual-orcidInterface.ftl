<#-- $This file is distributed under the terms of the license in /doc/license.txt$ -->

<#-- 
    If authorized to confirm ORCID IDs, add the function that will replace the add link.
    The OrcidIdDataGetter is attached to this template; it sets the orcidInfo structure. 
-->
 
<#if orcidInfo??>
    <#if orcidInfo.authorizedToConfirm>
        <script>
            $(document).ready(function(){
                $('#orcidId a.add-orcidId').replaceWith("<a class='add-orcidId' href='${orcidInfo.orcidUrl}'>Confirm/Create</a>");
            });
        </script>
    </#if>
</#if>
 