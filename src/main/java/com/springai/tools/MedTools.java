package com.springai.tools;

import com.springai.entity.Appointment;
import com.springai.entity.Doctor;
import com.springai.service.AppointmentService;
import com.springai.service.DoctorService;
import com.springai.service.ScheduleService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MedTools {
    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private DoctorService doctorService;
    @Autowired
    private ScheduleService scheduleService;

    @Transactional
    @Tool(name = "预约挂号", description = "根据参数，先执行工具方法queryDepartment查询是否可预约，并直接给用户回答是否可预约，并让用户确认所有预约信息，用户确认后再进行预约。")
    public String bookAppointment(Appointment appointment) {

        if (appointmentService.getOne(appointment) != null) {
            return "您在相同的科室和时间已有预约";
        }

        // 清掉ai幻觉
        appointment.setId(null);
        appointment.setScheduleId(null);


        if (!doctorService.registerIfAvailable(appointment)) {
            return "预约失败：该科室在所选日期和时段没有可预约的号源，请换时间或换医生";
        }

        // 写预约记录。
        try {
            if (!appointmentService.save(appointment)) {
                throw new IllegalStateException("预约记录写入返回 false");
            }
        } catch (Exception e) {
            // 与上面 claim 在同一个事务里，归还后净效果为零，事务可以正常提交
            scheduleService.release(appointment.getScheduleId());
            return "预约失败：写入预约记录时出错（" + e.getClass().getSimpleName() + "），号源已释放，请重试";
        }

        return "预约成功，并返回预约详情（科室：" + appointment.getDepartment()
                + "，医生：" + appointment.getDoctorName()
                + "，日期：" + appointment.getDate()
                + "，时段：" + appointment.getTime() + "）";
    }

    @Transactional
    @Tool(name = "取消预约挂号", description = "根据参数，查询预约是否存在，如果存在则删除预约记录并返回取消预约成功，否则返回取消预约失败")
    public String cancelAppointment(Appointment appointment) {
        Appointment appointmentDB = appointmentService.getOne(appointment);
        if (appointmentDB == null) {
            return "您没有预约记录，请核对预约科室和时间";
        }

        if (appointmentDB.getScheduleId() != null) {
            scheduleService.release(appointmentDB.getScheduleId());
        }
        if (!appointmentService.removeById(appointmentDB.getId())) {
            return "取消预约失败";
        }
        return "取消预约成功";
    }

    @Tool(name = "查询是否有号源", description = "判断某科室、某日期、某时段有没有号源，只回答有或没有。需要列出可推荐的医生，请改用「推荐医生」")
    public boolean queryDepartment(
            @ToolParam(description = "科室名称") String departmentName,
            @ToolParam(description = "日期") String date,
            @ToolParam(description = "时间，可选值：上午、下午") String time,
            @ToolParam(description = "医生名称", required = false) String doctorName
    ) {
        return doctorService.getAvailableSchedule(departmentName, date, time, doctorName) != null;
    }
    @Tool(name = "推荐医生",
            description = "按科室查询当前可以挂号的医生，用于向用户推荐医生。当用户说「推荐一位医生」「这个科室有哪些医生能挂」「我想找XX科的医生」时调用。"
                    + "日期或时段拿不到就留空，留空时只按科室筛。返回结果只是推荐，不代表号源已被锁定。")
    public String getDoctor(
            @ToolParam(description = "科室名称，例如：口腔科、神经内科") String departmentName,
            @ToolParam(description = "日期，格式 yyyy-MM-dd；用户没说就留空", required = false) String date,
            @ToolParam(description = "时段，可选值：上午、下午；用户没说就留空", required = false) String time) {
        List<Doctor> doctors = doctorService.getDoctorByDepartment(departmentName, date, time);
        if (doctors == null || doctors.isEmpty()) {
            return "科室「" + departmentName + "」当前没有可推荐的医生，请核对科室名称是否正确";
        }
        String scope = StringUtils.hasText(date) && StringUtils.hasText(time)
                ? "（" + date + " " + time + " 有余号）"
                : "";
        return "科室「" + departmentName + "」可推荐医生" + scope + "，共 " + doctors.size() + " 位：\n"
                + doctors.stream()
                        .map(d -> "- " + d.getName() + "（" + d.getTitle() + "），擅长：" + d.getSpecialty())
                        .collect(Collectors.joining("\n"));
    }
}
