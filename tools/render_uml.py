"""Rebuild the repository's UML SVG views using only Python's standard library."""
from html import escape
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class Diagram:
    def __init__(self, width, height, title, subtitle):
        self.parts = [f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
<defs>
 <marker id="triangle" markerWidth="14" markerHeight="14" refX="13" refY="7" orient="auto" markerUnits="userSpaceOnUse"><path d="M1,1 L13,7 L1,13 Z" fill="white" stroke="#334155" stroke-width="1.5"/></marker>
 <marker id="arrow" markerWidth="10" markerHeight="10" refX="9" refY="5" orient="auto" markerUnits="userSpaceOnUse"><path d="M1,1 L9,5 L1,9" fill="none" stroke="#334155" stroke-width="1.5"/></marker>
</defs>
<rect width="100%" height="100%" fill="white"/>
<g font-family="Arial, Helvetica, sans-serif" fill="#172033">
<text x="40" y="43" font-size="26" font-weight="bold">{escape(title)}</text>
<text x="40" y="72" font-size="15" fill="#475569">{escape(subtitle)}</text>''']
    def box(self, x,y,w,name,lines,kind='',abstract=False):
        head=54 if kind else 36
        h=head+len(lines)*23+16
        self.parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="0" fill="#f8fafc" stroke="#334155" stroke-width="1.5"/>')
        if kind:
            self.text(x+w/2,y+19,f'«{kind}»',13,anchor='middle')
        self.text(x+w/2,y+head-12,name,17,True,'middle',abstract)
        self.parts.append(f'<path d="M{x},{y+head} H{x+w}" stroke="#334155"/>')
        for i,line in enumerate(lines): self.text(x+12,y+head+23+i*23,line,14)
        return h
    def text(self,x,y,value,size=14,bold=False,anchor='start',italic=False):
        self.parts.append(f'<text x="{x}" y="{y}" font-size="{size}" text-anchor="{anchor}" font-weight="{"bold" if bold else "normal"}" font-style="{"italic" if italic else "normal"}">{escape(value)}</text>')
    def edge(self,points,kind='dependency',label=None,lx=0,ly=0):
        dash=' stroke-dasharray="7 5"' if kind in ('dependency','implements') else ''
        marker='triangle' if kind in ('extends','implements') else 'arrow'
        p=' '.join(f'{x},{y}' for x,y in points)
        self.parts.append(f'<polyline points="{p}" fill="none" stroke="#334155" stroke-width="1.6"{dash} marker-end="url(#{marker})"/>')
        if label: self.text(lx,ly,label,14)
    def write(self,path):
        (ROOT/path).write_text('\n'.join(self.parts)+"\n</g></svg>\n",encoding='utf-8')

p=Diagram(1500,1360,'Smart Hotel Room Service and Automation System',
          'UML class diagram 1 of 2   |   Service policy, delivery implementations, and shared data')
p.box(55,125,450,'RoomService',[
 '- provider: DeliveryProvider',
 '+ request(RoomServiceRequest): DeliveryReceipt',
 '# createOrder(RoomServiceRequest): DeliveryOrder'], 'abstract',True)
p.box(925,125,500,'DeliveryProvider',[
 '+ id(): String', '+ deliver(DeliveryOrder): DeliveryReceipt'], 'interface')
p.edge([(505,225),(925,225)],'association','provider 1',665,211)
p.box(35,390,360,'StandardRoomService',[
 '+ StandardRoomService(DeliveryProvider)',
 '# createOrder(request): DeliveryOrder'])
p.box(430,390,360,'QuietRoomService',[
 '+ QuietRoomService(DeliveryProvider)',
 '# createOrder(request): DeliveryOrder'])
p.edge([(215,390),(215,330),(220,330),(220,264)],'extends')
p.edge([(610,390),(610,310),(380,310),(380,264)],'extends')
p.box(835,390,280,'StaffDeliveryProvider',[
 '+ id(): String', '+ deliver(order): DeliveryReceipt'])
p.box(1180,390,280,'RobotDeliveryProvider',[
 '+ id(): String', '+ deliver(order): DeliveryReceipt'])
p.edge([(975,390),(975,280),(1070,280),(1070,241)],'implements')
p.edge([(1320,390),(1320,280),(1275,280),(1275,241)],'implements')
p.box(855,650,570,'LegacyDeliveryAdapter',[
 '- gateway: LegacyHotelGateway', '+ LegacyDeliveryAdapter()',
 '+ LegacyDeliveryAdapter(LegacyHotelGateway)',
 '+ id(): String', '+ deliver(order): DeliveryReceipt'])
p.edge([(1140,650),(1140,560),(1478,560),(1478,305),(1390,305),(1390,241)],'implements')
p.box(75,650,650,'LegacyHotelGateway',[
 '+ submit(sku: String, count: byte, roomCode: String, flags: int): String[]',
 '  throws LinkFault', '  nested LinkFault extends Exception'])
p.edge([(855,730),(725,730)],'association','gateway 1',740,713)
p.box(55,865,530,'DeliveryException',[
 '- reason: Reason', '+ reason(): Reason',
 'Reason: ROOM_NOT_FOUND, ITEM_UNAVAILABLE, BUSY,',
 'INVALID_ORDER, UNAVAILABLE, PROTOCOL_ERROR'])
p.text(60,1050,'extends java.lang.Exception; shared operational failure',14)
p.edge([(855,795),(780,795),(780,840),(620,840),(620,920),(585,920)],'dependency','throws',635,870)
p.box(650,910,350,'DeliveryOrder',[
 '+ request: RoomServiceRequest', '+ notification: NotificationMode'], 'record')
p.box(1085,910,350,'DeliveryReceipt',[
 '+ reference: String', '+ order: DeliveryOrder'], 'record')
p.edge([(1085,990),(1000,990)],'association','order 1',1010,974)
p.box(650,1130,350,'RoomServiceRequest',[
 '+ room: int', '+ amenity: Amenity', '+ quantity: int'], 'record')
p.edge([(825,1026),(825,1130)],'association','request 1',840,1090)
p.box(55,1125,220,'Amenity',['TOWELS','WATER','BREAKFAST'],'enum')
p.box(310,1125,275,'NotificationMode',['RING_BELL','SILENT'],'enum')
p.text(1085,1160,'Solid triangle line: inheritance',14)
p.text(1085,1185,'Dashed triangle line: interface realization',14)
p.text(1085,1210,'Solid open arrow: association',14)
p.text(1085,1235,'Dashed open arrow: dependency',14)
p.text(55,1330,'Operations use abbreviated parameter names where types are shown by related records. Constructors and utility members may be omitted.',14)
p.write(Path('docs/uml.svg'))

r=Diagram(1500,930,'Runtime registration and dependency injection',
          'UML class diagram 2 of 2   |   Application classes that select and construct the Bridge objects')
r.box(55,125,270,'Main',['+ main(String[]): void', '+ run(args, out, err): int'])
r.box(515,125,590,'RoomServiceApplication',[
 '- providers: Map<String, DeliveryProvider>', '- services: Map<String, RoomServiceFactory>',
 '+ RoomServiceApplication(providers, services)', '+ discover(): RoomServiceApplication',
 '+ request(mode, destination, amenity, quantity): DeliveryReceipt'])
r.edge([(325,180),(515,180)],'dependency','uses',402,162)
r.box(1150,435,290,'DeliveryProvider',['+ id(): String', '+ deliver(order): DeliveryReceipt'],'interface')
r.edge([(1105,220),(1295,220),(1295,435)],'association','providers 0..*',1150,203)
r.box(480,435,450,'RoomServiceFactory',[
 '+ id(): String', '+ create(DeliveryProvider): RoomService'],'interface')
r.edge([(760,292),(760,435)],'association','services 0..*',775,380)
r.box(35,690,350,'StandardServiceFactory',[
 '+ id(): String', '+ create(provider): RoomService'])
r.box(475,690,350,'QuietServiceFactory',[
 '+ id(): String', '+ create(provider): RoomService'])
r.edge([(320,690),(320,610),(595,610),(595,551)],'implements')
r.edge([(650,690),(650,551)],'implements')
r.box(35,435,360,'StandardRoomService',['+ StandardRoomService(provider)'])
r.edge([(210,690),(210,510)],'dependency','creates',226,650)
r.box(935,690,350,'QuietRoomService',['+ QuietRoomService(provider)'])
r.edge([(825,738),(935,738)],'dependency','creates',842,720)
r.text(55,855,'ServiceLoader discovers the implementations listed in META-INF/services. Each factory injects the provider selected from the destination.',16)
r.text(55,884,'Classes repeated from diagram 1 refer to the same types. Registry entries require unique lowercase IDs; no provider-specific switch is used.',14)
r.write(Path('docs/runtime-uml.svg'))
