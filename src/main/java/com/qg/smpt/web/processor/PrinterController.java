package com.qg.smpt.web.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.qg.smpt.printer.Compact;
import com.qg.smpt.util.OrderBuilder;
import com.qg.smpt.web.model.Constant;
import com.qg.smpt.web.model.Json.PrinterDetail;
import com.qg.smpt.web.repository.PrinterMapper;
import com.qg.smpt.web.repository.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.qg.smpt.share.ShareMem;
import com.qg.smpt.util.JsonUtil;
import com.qg.smpt.util.Level;
import com.qg.smpt.util.Logger;
import com.qg.smpt.web.model.Order;
import com.qg.smpt.web.model.Printer;
import com.qg.smpt.web.model.User;
import com.qg.smpt.web.service.UserService;

@Controller
public class PrinterController {
	private static final Logger LOGGER = Logger.getLogger(PrinterController.class);

	@Autowired
	private UserService userService;

	@Autowired
	private PrinterMapper printerMapper;

	@Autowired
	private UserMapper userMapper;

	@RequestMapping(value="/printer/{userId}", method=RequestMethod.GET, produces="application/json;charset=UTF-8")
	@ResponseBody
	public String seePrinterStatus(@PathVariable int userId) {

		// 从session中获取用户
//		HttpSession session = request.getSession();
//		User user = (User) session.getAttribute("user");
//		int userId = ((user != null) ? user.getId() : 0);


		LOGGER.log(Level.DEBUG, "查看用户[{0}]的打印机状态 ", userId);

		// 根据用户id获取打印机
		User user = ShareMem.userIdMap.get(userId);
		List<Printer> printers = null;
		// 若内存中没有用户，则去数据库中获取,并放进内存
		if(user == null) {
			user = userService.queryUserPrinter(userId);

			if(user != null && user.getPrinters() != null){
				synchronized (ShareMem.userIdMap) {
					ShareMem.userIdMap.put(user.getId(), user);
				}
			}
		}

		printers = user.getPrinters();
		String json = JsonUtil.jsonToMap(new String[]{"retcode","data"},
				new Object[]{1 ,printers});

		LOGGER.log(Level.DEBUG, "当前转化的信息为 [{0}]", json);

		return json;
	}

	@RequestMapping(value="/printer/status/{printerId}",  method=RequestMethod.GET, produces="application/json;charset=UTF-8" )
	@ResponseBody
	public String queryPrinter(@PathVariable int printerId) {
		// 根据打印机 id 获取打印机
		Printer printer = ShareMem.printerIdMap.get(printerId);
		PrinterDetail printerDetail = null;
		if(printer != null) {
			printerDetail = new PrinterDetail(printer);
		}else {
			LOGGER.debug("找不到id为" + printerId + "的打印机");
		}
		return JsonUtil.jsonToMap(new String[]{"printer"}, new Object[]{printerDetail});
	}

	@RequestMapping(value="/printer/{printerId}",  method=RequestMethod.DELETE, produces="application/json;charset=UTF-8" )
	@ResponseBody
	public String resetPrinter(@PathVariable int printerId) {
		// 根据打印机 id 获取打印机
		Printer printer = ShareMem.printerIdMap.get(printerId);

		// 若当前打印机存在，则将打印机的内部打印订单信息全重置
		if(printer != null) {
			synchronized (printer) {
				printer.reset();
			}
		}
		return JsonUtil.jsonToMap(new String[]{"status"}, new Object[]{"SUCCESS"});
	}

	/***
	 * 添加打印机
	 * @param
	 * @return
	 */
	@RequestMapping(value="/printer/add/{userId}/{printerId}",  method=RequestMethod.POST ,produces="application/json;charset=UTF-8")
	@ResponseBody
	public String addPrinter(@PathVariable int printerId, @PathVariable int userId) {
		Printer printer= new Printer();
		printer.setId(printerId);
		printer.setUserId(userId);
		printer.setPrinterStatus(String.valueOf((int)(Constant.PRINTER_HEATHY)));
		printerMapper.addPrinter(printer);
		printerMapper.addPrinterConnection(printer);

		User user = userMapper.selectByPrimaryKey(userId);
		user.setUserPrinters(user.getUserPrinters()+1);
		userMapper.updateByPrimaryKey(user);

		return JsonUtil.jsonToMap(new String[]{"status"}, new Object[]{"SUCCESS"});
	}


	/***
	 * 发送合同网数据报文
	 * @param
	 * @return
	 */
	@RequestMapping(value="/printer/sendCompact/{number}",  method=RequestMethod.GET ,produces="application/json;charset=UTF-8")
	@ResponseBody
	public String sendCompact(@PathVariable int number) {
		Compact compact = new Compact();
		List<Order> orders = new ArrayList<Order>();
		for (int i = 0; i<number; i++){
			orders.add(OrderBuilder.produceOrder(false,false));
		}
		compact.sendOrdersByCompact(1,orders);
		return JsonUtil.jsonToMap(new String[]{"status"}, new Object[]{"SUCCESS"});
	}

	/***
	 * 直接发送数据报文
	 * @param
	 * @return
	 */
	@RequestMapping(value="/printer/sendBulk/{flag}/{number}",  method=RequestMethod.GET ,produces="application/json;charset=UTF-8")
	@ResponseBody
	public String sendBulk(@PathVariable int number,@PathVariable int flag) {
		Compact compact = new Compact();
		List<Order> orders = new ArrayList<Order>();
		for (int i = 0; i<number; i++){
			orders.add(OrderBuilder.produceOrder(false,false));
		}
		compact.sendBulkDitectly(flag,orders);
		return JsonUtil.jsonToMap(new String[]{"status"}, new Object[]{"SUCCESS"});
	}

	/***
	 * 直接解约数据报文
	 * @param
	 * @return
	 */
	@RequestMapping(value="/printer/remove/{number}",  method=RequestMethod.GET ,produces="application/json;charset=UTF-8")
	@ResponseBody
	public String removeSign(@PathVariable int number) {
		Compact compact = new Compact();
		compact.removeSign(number);
		return JsonUtil.jsonToMap(new String[]{"status"}, new Object[]{"SUCCESS"});
	}
}
