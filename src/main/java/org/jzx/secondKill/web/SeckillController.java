package org.jzx.secondKill.web;

import org.jzx.secondKill.dto.Exposer;
import org.jzx.secondKill.dto.SeckillExecution;
import org.jzx.secondKill.dto.SeckillResult;
import org.jzx.secondKill.entity.SecKill;
import org.jzx.secondKill.enums.SeckillStateEnum;
import org.jzx.secondKill.exception.RepeatKillException;
import org.jzx.secondKill.exception.SeckillCloseException;
import org.jzx.secondKill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/seckill")
public class SeckillController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String list(Model model) {
        List<SecKill> list = seckillService.getSeckillList();
        model.addAttribute("list", list);

        return "list";
    }

    @RequestMapping(value = "/{seckillId}/detail", method = RequestMethod.GET)
    public String detail(@PathVariable Long seckillId, Model model) {
        if (seckillId == null) {
            return "redirect:/seckill/list";
        }
        SecKill secKill = seckillService.getById(seckillId);
        if (secKill == null) {
            return "forward:/seckill/list";
        }
        model.addAttribute("seckill", secKill);
        return "detail";
    }

    @RequestMapping(value = "/{seckillId}/exposer",
            method = RequestMethod.POST,
            produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<Exposer> exposer(@PathVariable Long seckillId) {
        SeckillResult<Exposer> result;

        try {
            Exposer exposer = seckillService.exportSeckillUrl(seckillId);
            result = new SeckillResult<Exposer>(true, exposer);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result = new SeckillResult<Exposer>(false, e.getMessage());
        }

        return result;
    }

    @RequestMapping(value = "/{seckillId}/{md5}/execution",
            method = RequestMethod.POST,
            produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<SeckillExecution> execute(@PathVariable("seckillId") Long seckillId,
                                                   @PathVariable("md5") String md5,
                                                   @CookieValue(value = "killPhone", required = false) Long phone) {
        SeckillResult<SeckillExecution> result;

        if (phone == null) {
            return new SeckillResult<SeckillExecution>(false, "未注册！");
        }
        try {
            SeckillExecution execution = seckillService.executeSeckill(seckillId, phone, md5);
            return new SeckillResult<SeckillExecution>(true, execution);
        } catch (RepeatKillException e) {
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
            return new SeckillResult<SeckillExecution>(true, execution);
        } catch (SeckillCloseException e) {
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.END);
            return new SeckillResult<SeckillExecution>(true, execution);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
            return new SeckillResult<SeckillExecution>(true, execution);
        }
    }

    @RequestMapping(value="/time/now", method = RequestMethod.GET)
    @ResponseBody
    public SeckillResult<Long> time(){
        return new SeckillResult<Long>(true, new Date().getTime());
    }
}
