unit Form_QREmply25;

interface

uses
  Windows,
  Messages,
  SysUtils,
  Variants,
  Classes,
  Graphics,
  Controls,
  Forms,
  Dialogs,
  QuickRpt,
  QRCtrls,
  ExtCtrls;

type
  TFormQREmply25 = class(TForm)
    QuickRep1: TQuickRep;
    DetailBand1: TQRBand;
    Qlb_Title: TQRLabel;
    QRLabel2: TQRLabel;
    QRLabel3: TQRLabel;
    QRLabel4: TQRLabel;
    QRLabel5: TQRLabel;
    QRLabel6: TQRLabel;
    QRLabel7: TQRLabel;
    QRShape1: TQRShape;
    QRShape2: TQRShape;
    QRShape3: TQRShape;
    Qlb: TQRLabel;
    QRShape4: TQRShape;
    QRShape5: TQRShape;
    QRShape6: TQRShape;
    Qlb_2: TQRLabel;
    QRShape7: TQRShape;
    Qlb_3: TQRLabel;
    QRShape8: TQRShape;
    QRShape9: TQRShape;
    QRShape10: TQRShape;
    QRShape11: TQRShape;
    Qlb_4: TQRLabel;
    Qlb_5: TQRLabel;
    QRShape12: TQRShape;
    Qlb_6: TQRLabel;
    Qlb_7: TQRLabel;
    Qlb_N_10: TQRLabel;
    QRShape13: TQRShape;
    QRShape14: TQRShape;
    Qlb_8: TQRLabel;
    QRShape15: TQRShape;
    Qlb_9: TQRLabel;
    Qlb_10: TQRLabel;
    QRShape16: TQRShape;
    QRShape17: TQRShape;
    QRShape18: TQRShape;
    QRShape19: TQRShape;
    Qlb_11: TQRLabel;
    QRShape20: TQRShape;
    Qlb_12: TQRLabel;
    QRShape21: TQRShape;
    Qlb_cm: TQRLabel;
    QRShape22: TQRShape;
    Qlb_13: TQRLabel;
    QRShape23: TQRShape;
    QRShape24: TQRShape;
    Qlb_14: TQRLabel;
    QRShape25: TQRShape;
    QRLabel24: TQRLabel;
    QRShape26: TQRShape;
    Qlb_mmHg: TQRLabel;
    QRShape27: TQRShape;
    QRLabel26: TQRLabel;
    QRShape28: TQRShape;
    Qlb_min: TQRLabel;
    QRShape29: TQRShape;
    QRShape30: TQRShape;
    QRShape31: TQRShape;
    QRShape32: TQRShape;
    QRShape33: TQRShape;
    Qlb_15: TQRLabel;
    Qlb_kg: TQRLabel;
    Qlb_16: TQRLabel;
    QRLabel31: TQRLabel;
    Qlb_mmHg_2: TQRLabel;
    QRLabel33: TQRLabel;
    Qlb_min_2: TQRLabel;
    QRShape34: TQRShape;
    Qlb_17: TQRLabel;
    Qlb_km: TQRLabel;
    Qlb_18: TQRLabel;
    Qlb_19: TQRLabel;
    Qlb_20: TQRLabel;
    Qlb_21: TQRLabel;
    Qlb_22: TQRLabel;
    QRShape35: TQRShape;
    Qlb_23: TQRLabel;
    Qlb_cm_2: TQRLabel;
    Qlb_24: TQRLabel;
    Qlb_25: TQRLabel;
    Qlb_26: TQRLabel;
    Qlb_27: TQRLabel;
    QRShape36: TQRShape;
    Qlb_28: TQRLabel;
    QRShape37: TQRShape;
    QRShape38: TQRShape;
    QRShape39: TQRShape;
    Qlb_N_66_70_80: TQRLabel;
    Qlb_29: TQRLabel;
    Qlb_30: TQRLabel;
    Qlb_31: TQRLabel;
    Qlb_32: TQRLabel;
    Qlb_33: TQRLabel;
    Qlb_34: TQRLabel;
    Qlb_35: TQRLabel;
    Qlb_36: TQRLabel;
    Qlb_37: TQRLabel;
    Qlb_38: TQRLabel;
    QRShape40: TQRShape;
    QRShape41: TQRShape;
    Qlb_39: TQRLabel;
    Qlb_40: TQRLabel;
    Qlb_41: TQRLabel;
    Qlb_42: TQRLabel;
    Qlb_43: TQRLabel;
    QRShape42: TQRShape;
    QRLabel65: TQRLabel;
    QRLabel66: TQRLabel;
    QRLabel67: TQRLabel;
    QRLabel68: TQRLabel;
    QRLabel69: TQRLabel;
    QRShape43: TQRShape;
    QRShape44: TQRShape;
    QRShape45: TQRShape;
    QRShape46: TQRShape;
    Qlb_44: TQRLabel;
    QRShape47: TQRShape;
    Qlb_45: TQRLabel;
    QRShape48: TQRShape;
    Qlb_46: TQRLabel;
    QRShape49: TQRShape;
    QRShape50: TQRShape;
    Qlb_47: TQRLabel;
    Qlb_48: TQRLabel;
    QRShape51: TQRShape;
    Qlb_49: TQRLabel;
    Qlb_50: TQRLabel;
    QRShape52: TQRShape;
    Qlb_51: TQRLabel;
    Qlb_52: TQRLabel;
    QRShape53: TQRShape;
    Qlb_B: TQRLabel;
    Qlb_53: TQRLabel;
    QRShape54: TQRShape;
    QRLabel81: TQRLabel;
    QRLabel82: TQRLabel;
    QRLabel83: TQRLabel;
    QRShape55: TQRShape;
    QRShape56: TQRShape;
    QRShape57: TQRShape;
    QRShape58: TQRShape;
    Qlb_54: TQRLabel;
    QRShape59: TQRShape;
    Qlb_55: TQRLabel;
    QRShape60: TQRShape;
    Qlb_56: TQRLabel;
    QRShape61: TQRShape;
    Qlb_57: TQRLabel;
    QRShape62: TQRShape;
    Qlb_58: TQRLabel;
    QRShape63: TQRShape;
    Qlb_59: TQRLabel;
    QRShape64: TQRShape;
    QRShape65: TQRShape;
    QRShape66: TQRShape;
    Qlb_60: TQRLabel;
    Qlb_61: TQRLabel;
    QRShape67: TQRShape;
    Qlb_62: TQRLabel;
    Qlb_63: TQRLabel;
    QRShape68: TQRShape;
    Qlb_1_2: TQRLabel;
    Qlb_64: TQRLabel;
    QRShape69: TQRShape;
    Qlb_OP: TQRLabel;
    Qlb_65: TQRLabel;
    QRShape70: TQRShape;
    Qlb_CT: TQRLabel;
    Qlb_66: TQRLabel;
    QRShape71: TQRShape;
    Qlb_67: TQRLabel;
    Qlb_68: TQRLabel;
    QRShape72: TQRShape;
    QRShape73: TQRShape;
    Qlb_69: TQRLabel;
    Qlb_70: TQRLabel;
    Qlb_71: TQRLabel;
    QRShape74: TQRShape;
    Qlb_72: TQRLabel;
    Qlb_73: TQRLabel;
    QRShape75: TQRShape;
    QRShape76: TQRShape;
    Qlb_74: TQRLabel;
    Qlb_75: TQRLabel;
    QRShape77: TQRShape;
    QRShape78: TQRShape;
    Qlb_76: TQRLabel;
    Qlb_77: TQRLabel;
    Qlb_Lipid_4: TQRLabel;
    QRShape79: TQRShape;
    QRShape80: TQRShape;
    Qlb_78: TQRLabel;
    Qlb_79: TQRLabel;
    Qlb_80: TQRLabel;
    QRShape81: TQRShape;
    Qlb_81: TQRLabel;
    QRShape82: TQRShape;
    Qlb_82: TQRLabel;
    Qlb_83: TQRLabel;
    QRShape83: TQRShape;
    QRShape84: TQRShape;
    QRShape85: TQRShape;
    QRShape86: TQRShape;
    Qlb_84: TQRLabel;
    QRShape87: TQRShape;
    Qlb_85: TQRLabel;
    QRShape88: TQRShape;
    Qlb_86: TQRLabel;
    QRShape89: TQRShape;
    Qlb_87: TQRLabel;
    QRShape90: TQRShape;
    Qlb_88: TQRLabel;
    QRShape91: TQRShape;
    Qlb_89: TQRLabel;
    QRShape92: TQRShape;
    Qlb_90: TQRLabel;
    QRShape93: TQRShape;
    QRShape94: TQRShape;
    QRShape95: TQRShape;
    QRShape96: TQRShape;
    QRShape97: TQRShape;
    Qlb_BX: TQRLabel;
    Qlb_CLO: TQRLabel;
    Qlb_BX_2: TQRLabel;
    Qlb_EMR: TQRLabel;
    Qlb_PP: TQRLabel;
    QRShape98: TQRShape;
    QRLabel130: TQRLabel;
    QRImage1: TQRImage;
  private
  public
  end;

var
  Form_QREmply25: TFormQREmply25;

implementation

{$R *.dfm}

end.