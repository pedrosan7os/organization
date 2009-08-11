/*
 * @(#)OrganizationManagementAction.java
 *
 * Copyright 2009 Instituto Superior Tecnico
 * Founding Authors: João Figueiredo, Luis Cruz, Paulo Abrantes, Susana Fernandes
 * 
 *      https://fenix-ashes.ist.utl.pt/
 * 
 *   This file is part of the Organization Module for the MyOrg web application.
 *
 *   The Organization Module is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.*
 *
 *   The Organization Module is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with the Organization Module. If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package module.organization.presentationTier.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import module.organization.domain.AccountabilityType;
import module.organization.domain.OrganizationalModel;
import module.organization.domain.Party;
import module.organization.domain.Person;
import module.organization.domain.UnconfirmedAccountability;
import module.organization.domain.Unit;
import module.organization.domain.UnitBean;
import module.organization.domain.dto.OrganizationalModelBean;
import module.organization.domain.search.PartySearchBean;
import myorg.domain.exceptions.DomainException;
import myorg.presentationTier.LayoutContext;
import myorg.presentationTier.actions.ContextBaseAction;
import myorg.presentationTier.component.OrganizationChart;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixWebFramework.struts.annotations.Mapping;

@Mapping(path = "/organizationModel")
public class OrganizationModelAction extends ContextBaseAction {

    public static class OrganizationalModelChart extends OrganizationChart<OrganizationalModel> {

	public OrganizationalModelChart(final Collection<OrganizationalModel> organizationalModels) {
	    super(sortCollection(organizationalModels), 3);
	}

	private static Collection sortCollection(final Collection<OrganizationalModel> organizationalModels) {
	    final List<OrganizationalModel> result = new ArrayList<OrganizationalModel>(organizationalModels);
	    Collections.sort(result, OrganizationalModel.COMPARATORY_BY_NAME);
	    return result;
	}

    }

    public static class PartyChart extends OrganizationChart<Party> {

	public PartyChart(final Collection<Party> parties) {
	    super(sortCollection(parties), 2);
	}

	public PartyChart(final Set<AccountabilityType> accountabilityTypes, final Party party) {
	    super(party, sortCollection(party.getParents(accountabilityTypes)), sortCollection(party.getChildren(accountabilityTypes)), 2);
	}

	private static Collection sortCollection(final Collection<Party> parties) {
	    final List<Party> result = new ArrayList<Party>(parties);
	    Collections.sort(result, Party.COMPARATOR_BY_NAME);
	    return result;
	}

	public Person getPerson() {
	    final Party party = getElement();
	    return party instanceof Person ? ((Person) party) : null;
	}

	public Unit getUnit() {
	    final Party party = getElement();
	    return party instanceof Unit ? ((Unit) party) : null;
	}
    }

    public static class PartyChartView extends PartyViewHook {

	@Override
	public String getViewName() {
	    return "default";
	}

	protected Set<AccountabilityType> getAccountabilityTypes(final OrganizationalModel organizationalModel) {
	    final Set<AccountabilityType> accountabilityTypes = new HashSet<AccountabilityType>();
	    accountabilityTypes.addAll(organizationalModel.getAccountabilityTypesSet());
	    accountabilityTypes.add(UnconfirmedAccountability.readAccountabilityType());
	    return accountabilityTypes;
	}


	@Override
	public String hook(final HttpServletRequest request, final OrganizationalModel organizationalModel, final Party party) {
	    final PartyChart partyChart = new PartyChart(getAccountabilityTypes(organizationalModel), party);
	    request.setAttribute("partyChart", partyChart);
	    return "/organization/model/partyChart.jsp";
	}

    }

    private static final PartyViewHookManager partyViewHookManager = new PartyViewHookManager();

    static {
	partyViewHookManager.register(new PartyChartView());
    }

    @Override
    public ActionForward execute(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final ActionForward forward = super.execute(mapping, form, request, response);
	final LayoutContext layoutContext = (LayoutContext) getContext(request);
	request.setAttribute("previousLayoutContextHead", layoutContext.getHead());
	layoutContext.setHead("/organization/layoutContext/head.jsp");
	return forward;
    }

    public ActionForward viewModels(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final Set<OrganizationalModel> organizationalModels = new TreeSet<OrganizationalModel>(OrganizationalModel.COMPARATORY_BY_NAME);
	organizationalModels.addAll(getMyOrg().getOrganizationalModelsSet());
	request.setAttribute("organizationalModels", organizationalModels);
	final OrganizationalModelChart organizationalModelChart = new OrganizationalModelChart(organizationalModels);
	request.setAttribute("organizationalModelChart", organizationalModelChart);
	return forward(request, "/organization/model/viewModels.jsp");
    }

    public ActionForward viewModel(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final OrganizationalModel organizationalModel = getDomainObject(request, "organizationalModelOid");
	request.setAttribute("organizationalModel", organizationalModel);

	Party party;
	PartySearchBean partySearchBean = getRenderedObject("partyBean");
	if (partySearchBean == null) {
	    party = getDomainObject(request, "partyOid");
	    if (party == null && organizationalModel.getPartiesCount() == 1) {
		party = organizationalModel.getPartiesIterator().next();
		request.setAttribute("viewName", "default");
	    }
	    partySearchBean = new PartySearchBean(party);
	} else {
	    party = partySearchBean.getParty();
	    RenderUtils.invalidateViewState();
	}
	request.setAttribute("partySearchBean", partySearchBean);

	if (party == null) {
	    final PartyChart partyChart = party == null ? new PartyChart(organizationalModel.getPartiesSet()) : new PartyChart(organizationalModel.getAccountabilityTypesSet(), party);
	    request.setAttribute("partiesChart", partyChart);
	} else {
	    request.setAttribute("party", party);
	    partyViewHookManager.hook(request, organizationalModel, party);

	    final SortedSet<PartyViewHook> hooks = partyViewHookManager.getSortedHooks(party);
	    if (hooks.size() > 1) {
		request.setAttribute("hooks", hooks);
	    }
	}

	return forward(request, "/organization/model/viewModel.jsp");
    }

    public ActionForward prepareCreateModel(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final OrganizationalModelBean organizationalModelBean = new OrganizationalModelBean();
	request.setAttribute("organizationalModelBean", organizationalModelBean);
	return forward(request, "/organization/model/createModel.jsp");
    }

    public ActionForward createModel(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final OrganizationalModelBean organizationalModelBean = getRenderedObject();
	OrganizationalModel.createNewModel(organizationalModelBean);
	return viewModels(mapping, form, request, response);
    }

    public ActionForward editModel(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final OrganizationalModel organizationalModel = getDomainObject(request, "organizationalModelOid");
	request.setAttribute("organizationalModel", organizationalModel);
	return forward(request, "/organization/model/editModel.jsp");
    }

    public ActionForward deleteModel(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final OrganizationalModel organizationalModel = getDomainObject(request, "organizationalModelOid");
	organizationalModel.delete();
	return viewModels(mapping, form, request, response);
    }

    public ActionForward prepareAddUnitToModel(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final OrganizationalModel organizationalModel = getDomainObject(request, "organizationalModelOid");
	request.setAttribute("organizationalModel", organizationalModel);
	final PartySearchBean partySearchBean = new PartySearchBean(null); 
	request.setAttribute("partySearchBean", partySearchBean);
	return forward(request, "/organization/model/addUnitToModel.jsp");
    }

    public ActionForward addUnitToModel(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final OrganizationalModel organizationalModel = getDomainObject(request, "organizationalModelOid");
	request.setAttribute("organizationalModel", organizationalModel);
	final PartySearchBean partySearchBean = getRenderedObject();
	organizationalModel.addPartyService(partySearchBean.getParty());
	return viewModel(mapping, form, request, response);
    }

    public ActionForward manageModelAccountabilityTypes(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final OrganizationalModel organizationalModel = getDomainObject(request, "organizationalModelOid");
	request.setAttribute("organizationalModel", organizationalModel);
	return forward(request, "/organization/model/manageModelAccountabilityTypes.jsp");
    }

    public ActionForward prepareCreateUnit(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
            final HttpServletResponse response) {
	final OrganizationalModel organizationalModel = getDomainObject(request, "organizationalModelOid");
	request.setAttribute("organizationalModel", organizationalModel);
	final Party party = getDomainObject(request, "partyOid");
	final UnitBean unitBean = new UnitBean();
	if (party != null) {
	    request.setAttribute("party", party);
	    unitBean.setParent((Unit) party);
	}
	request.setAttribute("unitBean", unitBean);
        return forward(request, "/organization/model/createUnit.jsp");
    }

    public ActionForward createUnit(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final UnitBean bean = getRenderedObject("unitBean");
	final OrganizationalModel organizationalModel = getDomainObject(request, "organizationalModelOid");
	if (bean.getParent() == null) {
	    bean.setOrganizationalModel(organizationalModel);
	}
	try {
	    bean.createUnit();
	    return viewModel(mapping, form, request, response);
	} catch (final DomainException e) {
	    addMessage(request, e.getKey(), e.getArgs());
	    request.setAttribute("organizationalModel", organizationalModel);
	    final Party party = getDomainObject(request, "partyOid");
	    request.setAttribute("party", party);
	    request.setAttribute("unitBean", bean);
	    return forward(request, "/organization/model/createUnit.jsp");
	}
    }

    public ActionForward prepareEditUnit(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final OrganizationalModel organizationalModel = getDomainObject(request, "organizationalModelOid");
	request.setAttribute("organizationalModel", organizationalModel);
	final Party party = getDomainObject(request, "partyOid");
	request.setAttribute("party", party);
	request.setAttribute("unitBean", new UnitBean((Unit) party));
	return forward(request, "/organization/model/editUnit.jsp");
    }

    public ActionForward editUnit(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final UnitBean bean = getRenderedObject("unitBean");
	try {
	    bean.editUnit();
	    return viewModel(mapping, form, request, response);
	} catch (final DomainException e) {
	    addMessage(request, e.getKey(), e.getArgs());
	    final OrganizationalModel organizationalModel = getDomainObject(request, "organizationalModelOid");
	    request.setAttribute("organizationalModel", organizationalModel);
	    final Party party = getDomainObject(request, "partyOid");
	    request.setAttribute("party", party);
	    request.setAttribute("unitBean", bean);
	    return forward(request, "/organization/model/editUnit.jsp");
	}
    }

    public ActionForward managePartyPartyTypes(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final OrganizationalModel organizationalModel = getDomainObject(request, "organizationalModelOid");
	request.setAttribute("organizationalModel", organizationalModel);
	final Party party = getDomainObject(request, "partyOid");
	request.setAttribute("party", party);
	return forward(request, "/organization/model/managePartyPartyTypes.jsp");
    }

    public ActionForward prepareAddUnit(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final OrganizationalModel organizationalModel = getDomainObject(request, "organizationalModelOid");
	request.setAttribute("organizationalModel", organizationalModel);
	final Party party = getDomainObject(request, "partyOid");
	request.setAttribute("party", party);
	final UnitBean unitBean = new UnitBean();
	unitBean.setParent(party);
	request.setAttribute("unitBean", unitBean);
	return forward(request, "/organization/model/addUnit.jsp");
    }

    public ActionForward addUnit(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final UnitBean unitBean = getRenderedObject("unitBean");
	try {
	    unitBean.addChild();
	    return viewModel(mapping, form, request, response);
	} catch (final DomainException e) {
	    addMessage(request, e.getKey(), e.getArgs());
	    final OrganizationalModel organizationalModel = getDomainObject(request, "organizationalModelOid");
	    request.setAttribute("organizationalModel", organizationalModel);
	    final Party party = unitBean.getParent();
	    request.setAttribute("party", party);
	    request.setAttribute("unitBean", unitBean);
	    return forward(request, "/organization/model/addUnit.jsp");
	}
    }

    public ActionForward deleteUnit(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
	    final HttpServletResponse response) throws Exception {
	final Unit unit = getDomainObject(request, "partyOid");
	try {
	    unit.delete();

	    final OrganizationalModel organizationalModel = getDomainObject(request, "organizationalModelOid");
	    request.setAttribute("organizationalModel", organizationalModel);

	    final PartySearchBean partySearchBean = new PartySearchBean(null);
	    request.setAttribute("partySearchBean", partySearchBean);

	    final PartyChart partyChart = new PartyChart(organizationalModel.getPartiesSet());
	    request.setAttribute("partiesChart", partyChart);

	    return forward(request, "/organization/model/viewModel.jsp");
	} catch (final DomainException e) {
	    addMessage(request, e.getKey(), e.getArgs());
	}
	return viewModel(mapping, form, request, response);
    }

}
