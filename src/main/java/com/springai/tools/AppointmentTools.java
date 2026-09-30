package com.springai.tools;

import com.springai.entity.Appointment;
import com.springai.service.AppointmentService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppointmentTools {
    @Autowired
    private AppointmentService appointmentService;
    @Tool(name="预约挂号", description= "根据参数，先执行工具方法queryDepartment查询是否可预约，并直接给用户回答是否可预约，并让用户确认所有预约信息，用户确认后再进行预约。")
    public String bookAppointment(Appointment appointment){
        Appointment appointmentDB = appointmentService.getOne(appointment);
        if(appointmentDB == null){
            appointment.setId(null);//防止大模型幻觉设置了id
            if(appointmentService.save(appointment)){
                return "预约成功，并返回预约详情";
            }else{
                return "预约失败";
            }
        }
        return "您在相同的科室和时间已有预约";
    }
    @Tool(name="取消预约挂号", description= "根据参数，查询预约是否存在，如果存在则删除预约记录并返回取消预约成功，否则返回取消预约失败消预约成功，否则返回取消预约失败")
    public String cancelAppointment(Appointment appointment){
        Appointment appointmentDB = appointmentService.getOne(appointment);

        if(appointmentDB != null){

            if(appointmentService.removeById(appointmentDB.getId())){
                return "取消预约成功";
            }else{
                return "取消预约失败";
            }
        }
        return "您没有预约记录，请核对预约科室和时间";
    }
    @Tool(name = "查询是否有号源", description="根据科室名称，日期，时间和医生查询是否有号源，并返回给用户")
    public boolean queryDepartment(
            @ToolParam(description = "科室名称") String name,
            @ToolParam(description = "日期") String date,
            @ToolParam(description = "时间，可选值：上午、下午") String time,
            @ToolParam(description = "医生名称", required = false) String doctorName
    ) {
        System.out.println("查询是否有号源");
        System.out.println("科室名称：" + name);
        System.out.println("日期：" + date);
        System.out.println("时间：" + time);
        System.out.println("医生名称：" + doctorName);
        //TODO 维护医生的排班信息：
        return true;
    }
}