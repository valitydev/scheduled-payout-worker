drop table if exists pt.adjustment;
drop table if exists pt.chargeback;
drop table if exists pt.invoice;
drop table if exists pt.payment;
drop table if exists pt.refund;

alter table pt.shop_meta
drop column if exists has_payment_institution_acc_pay_tool;
