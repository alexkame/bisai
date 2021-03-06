package com.thinkgem.jeesite.modules.bisai.web;


import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kidinfor.fastweixin.api.response.GetUserInfoResponse;
import com.kidinfor.util.WeixinHelp;
import com.thinkgem.jeesite.common.config.Global;
import com.thinkgem.jeesite.common.persistence.Page;
import com.thinkgem.jeesite.common.utils.MD5Util;
import com.thinkgem.jeesite.common.web.BaseController;
import com.thinkgem.jeesite.modules.bisai.entity.Account;
import com.thinkgem.jeesite.modules.bisai.entity.Match;
import com.thinkgem.jeesite.modules.bisai.entity.MatchTypeNote;
import com.thinkgem.jeesite.modules.bisai.global.GlobalBuss;
import com.thinkgem.jeesite.modules.bisai.service.AccountService;
import com.thinkgem.jeesite.modules.bisai.service.MatchService;
import com.thinkgem.jeesite.modules.bisai.service.MatchTypeNoteService;
import com.thinkgem.jeesite.modules.wx.util.WeixinUtil;

import net.sf.json.JSONObject;

/**
 * 网站Controller
 * 
 * @author ThinkGem
 * @version 2013-5-29
 */
@Controller
@RequestMapping(value = "${frontPath}")
public class FrontController extends BaseController {
    @Autowired
    private AccountService accountService;
    @Autowired
    private MatchService matchService;
    @Autowired
	private MatchTypeNoteService matchTypeNoteService;
    /**
     * 网站首页
     */
    @RequestMapping
    public String index(HttpServletRequest request,Model model) {
    	HttpSession session = request.getSession();
//    	String openId = (String) session.getAttribute(WeixinHelp.OPENID);
//    	if(StringUtils.isEmpty(openId)){
    		try{
		    	JSONObject token = WeixinUtil.getUserToken(request.getParameter("code"));
		    	String openId ="oRbfiwvoOYpH-3bPn1_8GmRbUqJY";//(String) token.getString(WeixinHelp.OPENID);//
		        session.setAttribute(WeixinHelp.OPENID,openId);
		        //如果注册过或授权登陆过无需再次登陆
		        Account tAccount = accountService.getAccountByOpenId(openId);
		        if(tAccount==null) throw new RuntimeException();
		        request.getSession().setAttribute(GlobalBuss.CURRENTACCOUNT, tAccount);
    		}catch(Exception e){//没授权先授权登陆
    			return "modules/bisai/front/login";
    		}
//    	}
        //model.addAttribute("isIndex", true);
//        return "modules/bisai/front/index";
        model.addAttribute("ismy",true);
        return "redirect:"+Global.getFrontPath()+"/match.html";
    }

    /**
     * 现有页面下部的菜单保留，点击我的，如果没有登陆就显示到登陆界面。 登陆界面有手机号和密码，也要有微信授权登陆的按钮。
     * 有注册按钮，点击注册，直接使用微信进行注册。 要求设定手机号登陆和一个登陆密码。
     */
    @RequestMapping(value = "my${urlSuffix}")
    public String index(HttpServletRequest request) {
        // 判断用户是否登陆
        HttpSession session = request.getSession();
        if (session.getAttribute(GlobalBuss.CURRENTACCOUNT) != null) {// 已登陆
            return "modules/bisai/front/about";
        }
        else {// 未登录
            return "modules/bisai/front/login";
        }
    }

    /** 注册页面 */
    @RequestMapping(value = "regist${urlSuffix}")
    public String regist(HttpServletRequest request) {
        return "modules/bisai/front/register";
    }
   
    /** 微信授权 */
    @RequestMapping(value = "wxoauth")
    public String wxoauth(HttpServletRequest request,String state) {
    	HttpSession session = request.getSession();
        String openId = (String) session.getAttribute(WeixinHelp.OPENID);
        if(StringUtils.isEmpty(openId)){
        	return "modules/bisai/front/login";
        }
        Account tAccount = accountService.getAccountByOpenId(openId);
        if(tAccount==null){
            //第四步：拉取用户信息(需scope为 snsapi_userinfo)
        	GetUserInfoResponse account = WeixinHelp.getInstance().getUserInfo(openId);
            //保存用户信息
            tAccount = new Account();
            tAccount.setWxname(account.getNickname());
            String s=account.getSex().toString();
            if(s==null) s="0";
            tAccount.setSex(s);
            tAccount.setOpenid(openId);
            tAccount.setWxphoto(account.getHeadimgurl());
            accountService.save(tAccount);
            request.getSession().setAttribute(GlobalBuss.CURRENTACCOUNT, tAccount);
        }
        if(tAccount==null /*&& StringUtils.isEmpty(tAccount.getPhone())*/){
            //return "modules/bisai/front/register";
            return "modules/bisai/front/login";
        }
        session.setAttribute(GlobalBuss.CURRENTACCOUNT, tAccount);
        request.setAttribute("ismy", true);
        String fromURL = (String) session.getAttribute("fromURL");
        if(!StringUtils.isEmpty(fromURL)){
        	return "redirect:"+fromURL;
        }
        //如果没有手机进行手机注册页面，并关联进行
        //return "modules/bisai/front/about";
        return "redirect:mymatch.html";
    }
    @RequestMapping(value = "regist",method=RequestMethod.POST)
    public String registAccount(HttpServletRequest request,Account account){
        //判断手机是否注册
        Account tAccount = accountService.getAccountByPhone(account.getPhone());
        if(tAccount!=null){//已注册
            request.setAttribute("errorMsg", "当前手机号已被注册！");
            return "modules/bisai/front/register";
        }else{
        	HttpSession session =request.getSession();
        	Account accountc = (Account) session.getAttribute(GlobalBuss.CURRENTACCOUNT);
        	if(accountc==null){
        		accountc = account;
        	}
        	accountc.setPhone(account.getPhone());
        	accountc.setOpenid((String) session.getAttribute(WeixinHelp.OPENID));
        	accountc.setPassword(MD5Util.md5Hex(account.getPassword()));
            accountService.save(accountc);
            session.setAttribute(GlobalBuss.CURRENTACCOUNT, accountc);
            return "modules/bisai/front/about";
        }
    }
    @RequestMapping(value = "login",method=RequestMethod.POST)
    public String loginAccount(HttpServletRequest request,Account account) {
        account.setPassword(MD5Util.md5Hex(account.getPassword()));
        Account dbaccount = accountService.getAccountByPhone(account.getPhone());
        if(dbaccount==null){//账号不存在
            request.setAttribute(GlobalBuss.ACCOUNTERROR, true);
            return "modules/bisai/front/login"; 
        }else if(!dbaccount.getPassword().equals(account.getPassword())){//密码错误
            request.setAttribute(GlobalBuss.PASSWDERROR, true);
            return "modules/bisai/front/login"; 
        }
        request.getSession().setAttribute(GlobalBuss.CURRENTACCOUNT, dbaccount);
        return "modules/bisai/front/about"; 
    }
    
    @RequestMapping(value = "logout")
    public String logout(HttpServletRequest request ,HttpServletResponse response){
        request.getSession().removeAttribute(GlobalBuss.CURRENTACCOUNT);
        request.getSession().removeAttribute(WeixinHelp.OPENID);
        return "modules/bisai/front/logout";
    }
    /**賽事列表*/
    @RequestMapping(value = "match${urlSuffix}")
    public String match(HttpServletRequest request,HttpServletResponse response,Model model) {
        Account account = (Account) request.getSession().getAttribute(GlobalBuss.CURRENTACCOUNT);
        Match match = new Match();
        if(account!=null){//我的赛事
            match.setAccount(account);
            Page<Match> page = matchService.findPage(new Page<Match>(request, response), match); 
            model.addAttribute("mypage", page);
        }
        Page<Match> page = matchService.findAllMatch(new Page<Match>(request, response),match); 
        model.addAttribute("page", page);
        boolean ismy = "true".equals(request.getParameter("ismy"));
        model.addAttribute("ismy",ismy);
        return "modules/bisai/front/match";
    }
    @RequestMapping(value = "matchSearch${urlSuffix}")
    public String matchSearch(Match match,HttpServletRequest request,HttpServletResponse response,Model model) {
        Account account = (Account) request.getSession().getAttribute(GlobalBuss.CURRENTACCOUNT);
        if(account!=null){//我的赛事
            match.setAccount(account);
            Page<Match> page = matchService.findPage(new Page<Match>(request, response), match); 
            model.addAttribute("mypage", page);
        }
        Page<Match> page = matchService.findAllMatch(new Page<Match>(request, response),match); 
        model.addAttribute("page", page);
        model.addAttribute("smatch",match);
        return "modules/bisai/front/match";
    }
    /**赛事申请*/
    @RequestMapping(value = "apply${urlSuffix}")
    public String apply(HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (session.getAttribute(GlobalBuss.CURRENTACCOUNT) == null) {//请先登陆
        	String openId = (String) session.getAttribute(WeixinHelp.OPENID);//"oRbfiwvoOYpH-3bPn1_8GmRbUqJY";//
            //如果注册过或授权登陆过无需再次登陆
            Account tAccount = accountService.getAccountByOpenId(openId);
            if(tAccount==null){//我的赛事
            	 return "modules/bisai/front/login";
        	}
            session.setAttribute(WeixinHelp.OPENID,openId);
            session.setAttribute(GlobalBuss.CURRENTACCOUNT, tAccount);
        }
        return "modules/bisai/front/apply";
    }
    @RequestMapping(value="apply_s${urlSuffix}",method=RequestMethod.POST)
    public String apply_s(HttpServletRequest request,Match match,RedirectAttributes redirectAttributes){
        HttpSession session = request.getSession();
        Account account = (Account) session.getAttribute(GlobalBuss.CURRENTACCOUNT);
        if (account == null) {//请先登陆
            addMessage(redirectAttributes, "比赛申请成功，请耐心等待管理员审批。");
             return "modules/bisai/front/login";
        }
        Date time = new Date();
        match.setState("0");
        match.setCreatetime(time);
        match.setUpdatetime(time);
        match.setAccount(account);
        matchService.save(match);
        addMessage(redirectAttributes, "比赛申请成功，请耐心等待管理员审批。");
        return "redirect:mymatch.html";
    }
    @RequestMapping(value = "activity${urlSuffix}")
    public String activity(String id,HttpServletRequest request,Model model) {
        Match match = matchService.get(id);
        model.addAttribute("match", match);
        model.addAttribute("isall",request.getParameter("isall"));
        //根据状态进行跳转页面
        String state = match.getState();
        if(StringUtils.isEmpty(state)) throw new RuntimeException("状态码错误，请联系管理员");
        if("1".equals(request.getParameter("isall"))){
        	HttpSession session = request.getSession();
        	String openId = (String) session.getAttribute(WeixinHelp.OPENID);//"oRbfiwvoOYpH-3bPn1_8GmRbUqJY";//
            //如果注册过或授权登陆过无需再次登陆
            Account tAccount = accountService.getAccountByOpenId(openId);
            if(tAccount==null){//我的赛事
            	 return "modules/bisai/front/login";
        	}
            session.setAttribute(GlobalBuss.CURRENTACCOUNT, tAccount);
        	return "modules/bisai/front/activity";
        }else{
	        if("0".equals(state)){
	            List<String> orgs = Arrays.asList(match.getOrgs().split(","));
	            model.addAttribute("orgs", orgs);
	            List<String> contractors = Arrays.asList(match.getContractor().split(","));
	            model.addAttribute("contractors", contractors);
	            List<String> sponsors = Arrays.asList(match.getSponsors().split(","));
	            model.addAttribute("sponsors", sponsors);
	            return "modules/bisai/front/apply";
	        }else if("1".equals(state)){
	            return "modules/bisai/front/activity";
	        }else if("2".equals(state)){//查看成绩
	        	//获取大类
	        	MatchTypeNote matchTypeNode = new MatchTypeNote();
	        	matchTypeNode.setMatch(match);
	        	List<MatchTypeNote> typeList = matchTypeNoteService.findList(matchTypeNode);
	    		model.addAttribute("type", typeList.get(0).getBtype());
	    		model.addAttribute("stype", 0);
				return "modules/bisai/front/final";
	        }else if("-1".equals(state)){
	        	return "modules/bisai/front/apply";
	        }else{//查看排名
	        	return "modules/bisai/front/ranking";
	        }
        }
    }
    /**********************************************************************/
    /*****************************一期菜单单独处理*****************************/
    /**********************************************************************/
    @RequestMapping(value = "allmatch${urlSuffix}")
    public String allMatch(HttpServletRequest request,HttpServletResponse response,Model model) {
    	HttpSession session = request.getSession();
    	String openId = (String)session.getAttribute(WeixinHelp.OPENID);
    	if(StringUtils.isEmpty(openId)){
    		JSONObject token = WeixinUtil.getUserToken(request.getParameter("code"));
        	openId = token.getString(WeixinHelp.OPENID);
        	session.setAttribute(WeixinHelp.OPENID,openId);
    	}
    	session.setAttribute("fromURL", "allmatch.html");
        Match match = new Match();
        Page<Match> page = matchService.findAllMatch(new Page<Match>(request, response),match); 
        model.addAttribute("page", page);
        return "modules/bisai/front/allmatch";
    }
    @RequestMapping(value = "allmatchSearch${urlSuffix}")
    public String allmatchSearch(Match match,HttpServletRequest request,HttpServletResponse response,Model model) {
        Page<Match> page = matchService.findAllMatch(new Page<Match>(request, response),match); 
        model.addAttribute("page", page);
        model.addAttribute("smatch",match);
        return "modules/bisai/front/allmatch";
    }
    @RequestMapping(value = "mymatch${urlSuffix}")
    public String myMatch(HttpServletRequest request,HttpServletResponse response,Model model) {
    	HttpSession session = request.getSession();
    	session.setAttribute("fromURL", "mymatch.html");
    	Account tAccount = (Account) session.getAttribute(GlobalBuss.CURRENTACCOUNT);
        if (tAccount == null) {//请先登陆
        	JSONObject token = WeixinUtil.getUserToken(request.getParameter("code"));
        	String openId = token.getString(WeixinHelp.OPENID);//"oRbfiwvoOYpH-3bPn1_8GmRbUqJY";//
            session.setAttribute(WeixinHelp.OPENID,openId);
            //如果注册过或授权登陆过无需再次登陆
        	tAccount = accountService.getAccountByOpenId(openId);
            if(tAccount==null){//我的赛事
            	 return "modules/bisai/front/login";
        	}
            session.setAttribute(GlobalBuss.CURRENTACCOUNT, tAccount);
        }else{
        	session.setAttribute(WeixinHelp.OPENID,tAccount.getOpenid());
        }
        
        
        Match match = new Match();
        match.setAccount(tAccount);
        Page<Match> mypage = matchService.findPage(new Page<Match>(request, response), match); 
        model.addAttribute("mypage", mypage);
        
        Page<Match> page = matchService.findCanyuPage(new Page<Match>(request, response),tAccount); 
        model.addAttribute("page", page);
        return "modules/bisai/front/mymatch";
    }
    @RequestMapping(value = "mymatchSearch${urlSuffix}")
    public String mymatchSearch(Match match,HttpServletRequest request,HttpServletResponse response,Model model) {
        Account account = (Account) request.getSession().getAttribute(GlobalBuss.CURRENTACCOUNT);
        if(account!=null){//我的赛事
            match.setAccount(account);
            Page<Match> page = matchService.findPage(new Page<Match>(request, response), match); 
            model.addAttribute("mypage", page);
        }
        model.addAttribute("smatch",match);
        return "modules/bisai/front/mymatch";
    }
    @RequestMapping(value = "myapply${urlSuffix}")
    public String myapply(HttpServletRequest request) {
    	HttpSession session = request.getSession();
    	session.setAttribute("fromURL", "apply.html");
    	Account tAccount = (Account) session.getAttribute(GlobalBuss.CURRENTACCOUNT);
        if (tAccount == null) {//请先登陆
        	JSONObject token = WeixinUtil.getUserToken(request.getParameter("code"));
        	String openId = token.getString(WeixinHelp.OPENID);//"oRbfiwvoOYpH-3bPn1_8GmRbUqJY";//
            session.setAttribute(WeixinHelp.OPENID,openId);
            //如果注册过或授权登陆过无需再次登陆
        	tAccount = accountService.getAccountByOpenId(openId);
            if(tAccount==null){//我的赛事
            	 return "modules/bisai/front/login";
        	}
            session.setAttribute(GlobalBuss.CURRENTACCOUNT, tAccount);
        }else{
        	session.setAttribute(WeixinHelp.OPENID,tAccount.getOpenid());
        }
        
        
        return "redirect:"+Global.getFrontPath()+"/apply.html";
    }
    /**********************************************************************/
    /*****************************一期菜单单独处理结束**************************/
    /**********************************************************************/
}
