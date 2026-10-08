import Breadcrumb from "@/components/Common/Breadcrumb";

export default function AdminShell({ title, children }: {
  title: string; children: React.ReactNode;
}) {
  return <main>
    <Breadcrumb title={title} pages={[title]} rootLabel="Admin" rootHref="/admin" />
    <section className="bg-gray-2 py-8 sm:py-12">
      <div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
        {children}
      </div>
    </section>
  </main>;
}
